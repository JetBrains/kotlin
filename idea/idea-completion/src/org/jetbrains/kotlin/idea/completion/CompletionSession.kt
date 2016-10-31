/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ModuleOrigin
import org.jetbrains.kotlin.idea.caches.resolve.OriginCapability
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

class CompletionSessionConfiguration(
        val useBetterPrefixMatcherForNonImportedClasses: Boolean,
        val nonAccessibleDeclarations: Boolean,
        val javaGettersAndSetters: Boolean,
        val javaClassesNotToBeUsed: Boolean,
        val staticMembers: Boolean,
        val dataClassComponentFunctions: Boolean
)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
        useBetterPrefixMatcherForNonImportedClasses = parameters.invocationCount < 2,
        nonAccessibleDeclarations = parameters.invocationCount >= 2,
        javaGettersAndSetters = parameters.invocationCount >= 2,
        javaClassesNotToBeUsed = parameters.invocationCount >= 2,
        staticMembers = parameters.invocationCount >= 2,
        dataClassComponentFunctions = parameters.invocationCount >= 2
)

abstract class CompletionSession(
        protected val configuration: CompletionSessionConfiguration,
        protected val parameters: CompletionParameters,
        protected val toFromOriginalFileMapper: ToFromOriginalFileMapper,
        resultSet: CompletionResultSet
) {
    protected val position = parameters.position
    protected val file = position.containingFile as KtFile
    protected val resolutionFacade = file.getResolutionFacade()
    protected val moduleDescriptor = resolutionFacade.moduleDescriptor
    protected val project = position.project
    protected val isJvmModule = TargetPlatformDetector.getPlatform(parameters.originalFile as KtFile) == JvmPlatform
    protected val isDebuggerContext = file is KtCodeFragment

    protected val nameExpression: KtSimpleNameExpression?
    protected val expression: KtExpression?

    init {
        val reference = (position.parent as? KtSimpleNameExpression)?.mainReference
        if (reference != null) {
            if (reference.expression is KtLabelReferenceExpression) {
                this.nameExpression = null
                this.expression = reference.expression.parent.parent as? KtExpressionWithLabel
            }
            else {
                this.nameExpression = reference.expression
                this.expression = nameExpression
            }
        }
        else {
            this.nameExpression = null
            this.expression = null
        }
    }

    protected val bindingContext = CompletionBindingContextProvider.getInstance(project).getBindingContext(position, resolutionFacade)
    protected val inDescriptor = position.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

    private val kotlinIdentifierStartPattern = StandardPatterns.character().javaIdentifierStart().andNot(singleCharPattern('$'))
    private val kotlinIdentifierPartPattern = StandardPatterns.character().javaIdentifierPart().andNot(singleCharPattern('$'))

    protected val prefix = CompletionUtil.findIdentifierPrefix(
            parameters.position.containingFile,
            parameters.offset,
            kotlinIdentifierPartPattern or singleCharPattern('@'),
            kotlinIdentifierStartPattern)!!

    protected val prefixMatcher = CamelHumpMatcher(prefix)

    protected val descriptorNameFilter: (String) -> Boolean = prefixMatcher.asStringNameFilter()

    protected val isVisibleFilter: (DeclarationDescriptor) -> Boolean = { isVisibleDescriptor(it, completeNonAccessible = configuration.nonAccessibleDeclarations) }
    protected val isVisibleFilterCheckAlways: (DeclarationDescriptor) -> Boolean = { isVisibleDescriptor(it, completeNonAccessible = false) }

    protected val referenceVariantsHelper = ReferenceVariantsHelper(bindingContext,
                                                                    resolutionFacade,
                                                                    moduleDescriptor,
                                                                    isVisibleFilter,
                                                                    NotPropertiesService.getNotProperties(position))

    protected val callTypeAndReceiver: CallTypeAndReceiver<*, *>
    protected val receiverTypes: Collection<ReceiverType>?

    init {
        val (callTypeAndReceiver, receiverTypes) = detectCallTypeAndReceiverTypes()
        this.callTypeAndReceiver = callTypeAndReceiver
        this.receiverTypes = receiverTypes
    }

    protected val basicLookupElementFactory = BasicLookupElementFactory(project, InsertHandlerProvider(callTypeAndReceiver.callType) { expectedInfos })

    // LookupElementsCollector instantiation is deferred because virtual call to createSorter uses data from derived classes
    protected val collector: LookupElementsCollector by lazy(LazyThreadSafetyMode.NONE) {
        LookupElementsCollector(prefixMatcher, parameters, resultSet, createSorter(), (file as? KtCodeFragment)?.extraCompletionFilter)
    }

    protected val searchScope: GlobalSearchScope = getResolveScope(parameters.originalFile as KtFile)

    protected fun indicesHelper(mayIncludeInaccessible: Boolean): KotlinIndicesHelper {
        val filter = if (mayIncludeInaccessible) isVisibleFilter else isVisibleFilterCheckAlways
        return KotlinIndicesHelper(resolutionFacade,
                                   searchScope,
                                   filter,
                                   filterOutPrivate = !mayIncludeInaccessible,
                                   declarationTranslator = { toFromOriginalFileMapper.toSyntheticFile(it) },
                                   file = file)
    }

    private fun isVisibleDescriptor(descriptor: DeclarationDescriptor, completeNonAccessible: Boolean): Boolean {
        if (!configuration.javaClassesNotToBeUsed && descriptor is ClassDescriptor) {
            if (descriptor.importableFqName?.let(::isJavaClassNotToBeUsedInKotlin) == true) return false
        }

        if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val visible = descriptor.isVisible(position, callTypeAndReceiver.receiver as? KtExpression, bindingContext, resolutionFacade)
            if (visible) return true
            return completeNonAccessible && (!descriptor.isFromLibrary() || isDebuggerContext)
        }

        if (descriptor.isExcludedFromAutoImport(project, file)) return false

        return true
    }

    private fun DeclarationDescriptor.isFromLibrary(): Boolean {
        if (module.getCapability(OriginCapability) == ModuleOrigin.LIBRARY) return true

        if (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return overriddenDescriptors.all { it.isFromLibrary() }
        }

        return false
    }

    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
        val owner = typeParameter.containingDeclaration
        var parent: DeclarationDescriptor? = inDescriptor
        while (parent != null) {
            if (parent == owner) return true
            if (parent is ClassDescriptor && !parent.isInner) return false
            parent = parent.containingDeclaration
        }
        return true
    }

    protected fun flushToResultSet() {
        collector.flushToResultSet()
    }

    fun complete(): Boolean {
        // we restart completion when prefix becomes "get" or "set" to ensure that properties get lower priority comparing to get/set functions (see KT-12299)
        val prefixPattern = StandardPatterns.string().with(object : PatternCondition<String>("get or set prefix") {
            override fun accepts(prefix: String, context: ProcessingContext?) = prefix == "get" || prefix == "set"
        })
        collector.restartCompletionOnPrefixChange(prefixPattern)

        val statisticsContext = calcContextForStatisticsInfo()
        if (statisticsContext != null) {
            collector.addLookupElementPostProcessor { lookupElement ->
                // we should put data into the original element because of DecoratorCompletionStatistician
                lookupElement.putUserDataDeep(STATISTICS_INFO_CONTEXT_KEY, statisticsContext)
                lookupElement
            }
        }

        doComplete()
        flushToResultSet()
        return !collector.isResultEmpty
    }

    fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        collector.addLookupElementPostProcessor(processor)
    }

    protected abstract fun doComplete()

    protected abstract val descriptorKindFilter: DescriptorKindFilter?

    protected abstract val expectedInfos: Collection<ExpectedInfo>

    protected val importableFqNameClassifier = ImportableFqNameClassifier(file)

    protected open fun createSorter(): CompletionSorter {
        var sorter = CompletionSorter.defaultSorter(parameters, prefixMatcher)!!

        sorter = sorter.weighBefore("stats", DeprecatedWeigher, PriorityWeigher, PreferGetSetMethodsToPropertyWeigher,
                                    NotImportedWeigher(importableFqNameClassifier),
                                    NotImportedStaticMemberWeigher(importableFqNameClassifier),
                                    KindWeigher, CallableWeigher)

        sorter = sorter.weighAfter("stats", VariableOrFunctionWeigher, ImportedWeigher(importableFqNameClassifier))

        val preferContextElementsWeigher = PreferContextElementsWeigher(inDescriptor)
        if (callTypeAndReceiver is CallTypeAndReceiver.SUPER_MEMBERS) { // for completion after "super." strictly prefer the current member
            sorter = sorter.weighBefore("kotlin.deprecated", preferContextElementsWeigher)
        }
        else {
            sorter = sorter.weighBefore("kotlin.proximity", preferContextElementsWeigher)
        }

        sorter = sorter.weighBefore("middleMatching", PreferMatchingItemWeigher)

        // we insert one more RealPrefixMatchingWeigher because one inserted in default sorter is placed in a bad position (after "stats")
        sorter = sorter.weighAfter("lift.shorter", RealPrefixMatchingWeigher())

        sorter = sorter.weighAfter("kotlin.proximity", ByNameAlphabeticalWeigher, PreferLessParametersWeigher)

        return sorter
    }

    protected fun calcContextForStatisticsInfo(): String? {
        if (expectedInfos.isEmpty()) return null

        var context = expectedInfos
                .mapNotNull { it.fuzzyType?.type?.constructor?.declarationDescriptor?.importableFqName }
                .distinct()
                .singleOrNull()
                ?.let { "expectedType=$it" }

        if (context == null) {
            context = expectedInfos
                    .mapNotNull { it.expectedName }
                    .distinct()
                    .singleOrNull()
                    ?.let { "expectedName=$it" }
        }

        return context
    }

    protected val referenceVariantsCollector = if (nameExpression != null) {
        ReferenceVariantsCollector(referenceVariantsHelper, indicesHelper(true), prefixMatcher,
                                   nameExpression, callTypeAndReceiver, resolutionFacade, bindingContext,
                                   importableFqNameClassifier, configuration)
    }
    else {
        null
    }

    protected fun ReferenceVariants.excludeNonInitializedVariable(): ReferenceVariants {
        return ReferenceVariants(referenceVariantsHelper.excludeNonInitializedVariable(imported, position), notImportedExtensions)
    }

    protected fun referenceVariantsWithSingleFunctionTypeParameter(): ReferenceVariants? {
        val variants = referenceVariantsCollector?.allCollected ?: return null
        val filter = { descriptor: DeclarationDescriptor -> descriptor is FunctionDescriptor && LookupElementFactory.hasSingleFunctionTypeParameter(descriptor) }
        return ReferenceVariants(variants.imported.filter(filter), variants.notImportedExtensions.filter(filter))
    }

    protected fun getRuntimeReceiverTypeReferenceVariants(lookupElementFactory: LookupElementFactory): Pair<ReferenceVariants, LookupElementFactory>? {
        val evaluator = file.getCopyableUserData(KtCodeFragment.RUNTIME_TYPE_EVALUATOR) ?: return null
        val referenceVariants = referenceVariantsCollector?.allCollected ?: return null

        val explicitReceiver = callTypeAndReceiver.receiver as? KtExpression ?: return null
        val type = bindingContext.getType(explicitReceiver) ?: return null
        if (!TypeUtils.canHaveSubtypes(KotlinTypeChecker.DEFAULT, type)) return null

        val runtimeType = evaluator(explicitReceiver)
        if (runtimeType == null || runtimeType == type) return null

        val expressionReceiver = ExpressionReceiver.create(explicitReceiver, runtimeType, bindingContext)
        val (variants, notImportedExtensions) = ReferenceVariantsCollector(
                referenceVariantsHelper, indicesHelper(true), prefixMatcher,
                nameExpression!!, callTypeAndReceiver, resolutionFacade, bindingContext,
                importableFqNameClassifier, configuration, runtimeReceiver = expressionReceiver
        ).collectReferenceVariants(descriptorKindFilter!!)
        val filteredVariants = filterVariantsForRuntimeReceiverType(variants, referenceVariants.imported)
        val filteredNotImportedExtensions = filterVariantsForRuntimeReceiverType(notImportedExtensions, referenceVariants.notImportedExtensions)

        val runtimeVariants = ReferenceVariants(filteredVariants, filteredNotImportedExtensions)
        return Pair(runtimeVariants, lookupElementFactory.copy(receiverTypes = listOf(ReceiverType(runtimeType, 0))))
    }

    private fun <TDescriptor : DeclarationDescriptor> filterVariantsForRuntimeReceiverType(
            runtimeVariants: Collection<TDescriptor>,
            baseVariants: Collection<TDescriptor>
    ): Collection<TDescriptor> {
        val baseVariantsByName = baseVariants.groupBy { it.name }
        val result = ArrayList<TDescriptor>()
        for (variant in runtimeVariants) {
            val candidates = baseVariantsByName[variant.name]
            if (candidates == null || candidates.none { compareDescriptors(project, variant, it) }) {
                result.add(variant)
            }
        }
        return result
    }

    protected open fun shouldCompleteTopLevelCallablesFromIndex(): Boolean {
        if (nameExpression == null) return false
        if ((descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.CALLABLES_MASK) == 0) return false
        if (callTypeAndReceiver is CallTypeAndReceiver.IMPORT_DIRECTIVE) return false
        return callTypeAndReceiver.receiver == null
    }

    protected fun processTopLevelCallables(processor: (CallableDescriptor) -> Unit) {
        val shadowedFilter = ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, nameExpression!!, callTypeAndReceiver)
                ?.createNonImportedDeclarationsFilter<CallableDescriptor>(referenceVariantsCollector!!.allCollected.imported)
        indicesHelper(true).processTopLevelCallables({ prefixMatcher.prefixMatches(it) }) {
            if (shadowedFilter != null) {
                shadowedFilter(listOf(it)).singleOrNull()?.let(processor)
            }
            else {
                processor(it)
            }
        }
    }

    protected fun withCollectRequiredContextVariableTypes(action: (LookupElementFactory) -> Unit): Collection<FuzzyType> {
        val provider = CollectRequiredTypesContextVariablesProvider()
        val lookupElementFactory = createLookupElementFactory(provider)
        action(lookupElementFactory)
        return provider.requiredTypes
    }

    protected fun withContextVariablesProvider(contextVariablesProvider: ContextVariablesProvider, action: (LookupElementFactory) -> Unit) {
        val lookupElementFactory = createLookupElementFactory(contextVariablesProvider)
        action(lookupElementFactory)
    }

    protected open fun createLookupElementFactory(contextVariablesProvider: ContextVariablesProvider): LookupElementFactory {
        return LookupElementFactory(basicLookupElementFactory, receiverTypes,
                                    callTypeAndReceiver.callType, inDescriptor, contextVariablesProvider)
    }

    private fun detectCallTypeAndReceiverTypes(): Pair<CallTypeAndReceiver<*, *>, Collection<ReceiverType>?> {
        if (nameExpression == null) {
            return CallTypeAndReceiver.UNKNOWN to null
        }

        val callTypeAndReceiver = CallTypeAndReceiver.detect(nameExpression)

        var receiverTypes = callTypeAndReceiver.receiverTypesWithIndex(
                bindingContext, nameExpression, moduleDescriptor, resolutionFacade,
                stableSmartCastsOnly = true /* we don't include smart cast receiver types for "unstable" receiver value to mark members grayed */)

        if (callTypeAndReceiver is CallTypeAndReceiver.SAFE || isDebuggerContext) {
            receiverTypes = receiverTypes?.map { ReceiverType(it.type.makeNotNullable(), it.receiverIndex) }
        }

        return callTypeAndReceiver to receiverTypes
    }
}
