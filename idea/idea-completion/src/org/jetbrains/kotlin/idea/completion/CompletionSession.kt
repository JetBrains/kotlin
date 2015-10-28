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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.ImportableFqNameClassifier
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class CompletionSessionConfiguration(
        val completeNonImportedDeclarations: Boolean,
        val completeNonAccessibleDeclarations: Boolean,
        val filterOutJavaGettersAndSetters: Boolean,
        val completeJavaClassesNotToBeUsed: Boolean
)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
        completeNonImportedDeclarations = parameters.invocationCount >= 2,
        completeNonAccessibleDeclarations = parameters.invocationCount >= 2,
        filterOutJavaGettersAndSetters = parameters.invocationCount < 2,
        completeJavaClassesNotToBeUsed = parameters.invocationCount >= 2
)

abstract class CompletionSession(protected val configuration: CompletionSessionConfiguration,
                                     protected val parameters: CompletionParameters,
                                     resultSet: CompletionResultSet) {
    protected val position = parameters.getPosition()
    private val file = position.getContainingFile() as KtFile
    protected val resolutionFacade = file.getResolutionFacade()
    protected val moduleDescriptor = resolutionFacade.moduleDescriptor
    protected val project = position.getProject()
    protected val isJvmModule = !ProjectStructureUtil.isJsKotlinModule(parameters.originalFile as KtFile)

    protected val nameExpression: KtSimpleNameExpression?
    protected val expression: KtExpression?

    init {
        val reference = (position.getParent() as? KtSimpleNameExpression)?.mainReference
        if (reference != null) {
            if (reference.expression is KtLabelReferenceExpression) {
                this.nameExpression = null
                this.expression = reference.expression.getParent().getParent() as? KtExpressionWithLabel
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

    protected val bindingContext = resolutionFacade.analyze(position.parentsWithSelf.firstIsInstance<KtElement>(), BodyResolveMode.PARTIAL_FOR_COMPLETION)
    protected val inDescriptor = position.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

    private val kotlinIdentifierStartPattern = StandardPatterns.character().javaIdentifierStart() andNot singleCharPattern('$')
    private val kotlinIdentifierPartPattern = StandardPatterns.character().javaIdentifierPart() andNot singleCharPattern('$')

    protected val prefix = CompletionUtil.findIdentifierPrefix(
            parameters.getPosition().getContainingFile(),
            parameters.getOffset(),
            kotlinIdentifierPartPattern or singleCharPattern('@'),
            kotlinIdentifierStartPattern)

    protected val prefixMatcher = CamelHumpMatcher(prefix)

    protected val descriptorNameFilter: (Name) -> Boolean = run {
        val nameFilter = prefixMatcher.asNameFilter()
        val getOrSetPrefix = listOf("get", "set").firstOrNull { prefix.startsWith(it) }
        if (getOrSetPrefix != null)
            nameFilter or prefixMatcher.cloneWithPrefix(prefix.removePrefix(getOrSetPrefix).decapitalizeSmart()).asNameFilter()
        else
            nameFilter
    }

    private fun ((Name) -> Boolean).or(otherFilter: (Name) -> Boolean): (Name) -> Boolean
            = { this(it) || otherFilter(it) }

    protected val isVisibleFilter: (DeclarationDescriptor) -> Boolean = { isVisibleDescriptor(it) }

    protected val referenceVariantsHelper = ReferenceVariantsHelper(bindingContext, resolutionFacade, isVisibleFilter)

    protected val callTypeAndReceiver: CallTypeAndReceiver<*, *>
    protected val lookupElementFactory: LookupElementFactory

    init {
        val (callTypeAndReceiver, receiverTypes) = detectCallTypeAndReceiverTypes()
        this.callTypeAndReceiver = callTypeAndReceiver
        this.lookupElementFactory = createLookupElementFactory(callTypeAndReceiver.callType, receiverTypes)
    }

    // LookupElementsCollector instantiation is deferred because virtual call to createSorter uses data from derived classes
    protected val collector: LookupElementsCollector by lazy(LazyThreadSafetyMode.NONE) {
        LookupElementsCollector(prefixMatcher, parameters, resultSet, lookupElementFactory, createSorter())
    }

    protected val originalSearchScope: GlobalSearchScope = getResolveScope(parameters.getOriginalFile() as KtFile)

    // we need to exclude the original file from scope because our resolve session is built with this file replaced by synthetic one
    protected val searchScope: GlobalSearchScope = object : DelegatingGlobalSearchScope(originalSearchScope) {
        override fun contains(file: VirtualFile) = super.contains(file) && file != parameters.getOriginalFile().getVirtualFile()
    }

    protected val indicesHelper: KotlinIndicesHelper
        get() = KotlinIndicesHelper(resolutionFacade, searchScope, isVisibleFilter, true)

    protected val toFromOriginalFileMapper: ToFromOriginalFileMapper
            = ToFromOriginalFileMapper(parameters.originalFile as KtFile, position.containingFile as KtFile, parameters.offset)

    // excludes top-level extensions except for ones declared in the current file - those that are fetched from indices
    protected val topLevelExtensionsExclude = object : DescriptorKindExclude() {
        val extensionsFromThisFile = file.declarations
                .filter { it.isExtensionDeclaration() }
                .map { it.resolveToDescriptor() }
                .toSet()

        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            return descriptor is CallableMemberDescriptor
                   && descriptor.extensionReceiverParameter != null
                   && descriptor.containingDeclaration is PackageFragmentDescriptor
                   && descriptor !in extensionsFromThisFile
                   && descriptor.kind == CallableMemberDescriptor.Kind.DECLARATION /* do not filter out synthetic extensions from packages */
        }

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        if (!configuration.completeJavaClassesNotToBeUsed && descriptor is ClassDescriptor) {
            val classification = descriptor.importableFqName?.let { importableFqNameClassifier.classify(it, isPackage = false) }
            if (classification == ImportableFqNameClassifier.Classification.notToBeUsedInKotlin) return false
        }

        if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val visible = descriptor.isVisible(inDescriptor, bindingContext, nameExpression)
            if (visible) return true
            if (!configuration.completeNonAccessibleDeclarations) return false
            return DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) !is PsiCompiledElement
        }

        return true
    }

    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
        val owner = typeParameter.getContainingDeclaration()
        var parent: DeclarationDescriptor? = inDescriptor
        while (parent != null) {
            if (parent == owner) return true
            if (parent is ClassDescriptor && !parent.isInner()) return false
            parent = parent.getContainingDeclaration()
        }
        return true
    }

    protected fun flushToResultSet() {
        collector.flushToResultSet()
    }

    public fun complete(): Boolean {
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

    protected abstract fun doComplete()

    protected abstract val descriptorKindFilter: DescriptorKindFilter?

    protected abstract val expectedInfos: Collection<ExpectedInfo>

    protected val importableFqNameClassifier = ImportableFqNameClassifier(file)

    protected open fun createSorter(): CompletionSorter {
        var sorter = CompletionSorter.defaultSorter(parameters, prefixMatcher)!!

        sorter = sorter.weighBefore("stats", DeprecatedWeigher, PriorityWeigher, NotImportedWeigher(importableFqNameClassifier), KindWeigher, CallableWeigher)

        sorter = sorter.weighAfter("stats", VariableOrFunctionWeigher, ImportedWeigher(importableFqNameClassifier))

        sorter = sorter.weighBefore("middleMatching", PreferMatchingItemWeigher)

        return sorter
    }

    protected fun calcContextForStatisticsInfo(): String? {
        if (expectedInfos.isEmpty()) return null

        var context = expectedInfos
                .map { it.fuzzyType?.type?.constructor?.declarationDescriptor?.importableFqName }
                .filterNotNull()
                .distinct()
                .singleOrNull()
                ?.let { "expectedType=$it" }

        if (context == null) {
            context = expectedInfos
                    .map { it.expectedName }
                    .filterNotNull()
                    .distinct()
                    .singleOrNull()
                    ?.let { "expectedName=$it" }
        }

        return context
    }

    /* TODO: not protected because of KT-9809 */
    data class ReferenceVariants(val imported: Collection<DeclarationDescriptor>, val notImportedExtensions: Collection<CallableDescriptor>)

    protected val referenceVariants: ReferenceVariants? by lazy {
        if (descriptorKindFilter == null) return@lazy null

        var variants = referenceVariantsHelper.getReferenceVariants(
                nameExpression!!,
                descriptorKindFilter!!,
                descriptorNameFilter,
                filterOutJavaGettersAndSetters = false,
                filterOutShadowed = false)

        variants = variants.excludeNonInitializedVariable(nameExpression)

        val shadowedDeclarationsFilter = ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, position, callTypeAndReceiver)

        var notImportedExtensions: Collection<CallableDescriptor> = emptyList()
        if (callTypeAndReceiver.shouldCompleteCallableExtensions()) {
            val extensions = indicesHelper.getCallableTopLevelExtensions({ prefixMatcher.prefixMatches(it) }, callTypeAndReceiver, expression!!, bindingContext)
            val pair = extensions.partition { isImportableDescriptorImported(it) }
            variants += pair.first
            notImportedExtensions = pair.second
        }

        if (shadowedDeclarationsFilter != null) {
            variants = shadowedDeclarationsFilter.filter(variants)
            notImportedExtensions = shadowedDeclarationsFilter.filterNonImported(notImportedExtensions, variants)
        }

        if (configuration.filterOutJavaGettersAndSetters) {
            variants = referenceVariantsHelper.filterOutJavaGettersAndSetters(variants)
        }

        ReferenceVariants(variants, notImportedExtensions)
    }

    // filters out variable inside its initializer
    private fun Collection<DeclarationDescriptor>.excludeNonInitializedVariable(expression: KtExpression): Collection<DeclarationDescriptor> {
        for (element in expression.parentsWithSelf) {
            val parent = element.getParent()
            if (parent is KtVariableDeclaration && element == parent.getInitializer()) {
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent]
                return this.filter { it != descriptor }
            }
            if (element is KtDeclaration) break // we can use variable inside lambda or anonymous object located in its initializer
        }
        return this
    }

    protected fun getRuntimeReceiverTypeReferenceVariants(): Collection<DeclarationDescriptor> {
        val descriptors = referenceVariantsHelper.getReferenceVariants(
                nameExpression!!,
                descriptorKindFilter!!,
                descriptorNameFilter,
                useRuntimeReceiverType = true,
                filterOutJavaGettersAndSetters = configuration.filterOutJavaGettersAndSetters
        ).excludeNonInitializedVariable(nameExpression)
        return descriptors.filter { descriptor ->
            referenceVariants!!.imported.none { compareDescriptors(project, it, descriptor) }
        }
    }

    protected fun shouldCompleteTopLevelCallablesFromIndex(): Boolean {
        if (!configuration.completeNonImportedDeclarations) return false
        if ((descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.CALLABLES_MASK) == 0) return false
        if (callTypeAndReceiver is CallTypeAndReceiver.IMPORT_DIRECTIVE) return false
        return callTypeAndReceiver.receiver == null
    }

    protected fun getTopLevelCallables(): Collection<DeclarationDescriptor> {
        return indicesHelper.getTopLevelCallables({ prefixMatcher.prefixMatches(it) })
                .filterShadowedNonImported()
    }

    protected fun CallTypeAndReceiver<*, *>.shouldCompleteCallableExtensions(): Boolean {
        return callType.descriptorKindFilter.kindMask.and(DescriptorKindFilter.CALLABLES_MASK) != 0
               && this !is CallTypeAndReceiver.IMPORT_DIRECTIVE
    }

    private fun Collection<CallableDescriptor>.filterShadowedNonImported(): Collection<CallableDescriptor> {
        val filter = ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, nameExpression!!, callTypeAndReceiver)
        return if (filter != null) filter.filterNonImported(this, referenceVariants!!.imported) else this
    }

    protected fun addClassesFromIndex(kindFilter: (ClassKind) -> Boolean) {
        AllClassesCompletion(parameters, indicesHelper, prefixMatcher, resolutionFacade, kindFilter)
                .collect(
                        { descriptor -> collector.addDescriptorElements(descriptor, notImported = true) },
                        { javaClass -> collector.addElement(lookupElementFactory.createLookupElementForJavaClass(javaClass), notImported = true) }
                )
    }

    private fun createLookupElementFactory(callType: CallType<*>?, receiverTypes: Collection<KotlinType>?): LookupElementFactory {
        val contextVariablesProvider = {
            nameExpression?.let {
                referenceVariantsHelper.getReferenceVariants(it, CallTypeAndReceiver.DEFAULT, DescriptorKindFilter.VARIABLES, nameFilter = { true })
                        .map { it as VariableDescriptor }
            } ?: emptyList()
        }

        val insertHandlerProvider = InsertHandlerProvider(callType) { expectedInfos }
        return LookupElementFactory(resolutionFacade, receiverTypes,
                                    callType, expression?.parent is KtSimpleNameStringTemplateEntry,
                                    insertHandlerProvider, contextVariablesProvider)
    }

    private fun detectCallTypeAndReceiverTypes(): Pair<CallTypeAndReceiver<*, *>, Collection<KotlinType>?> {
        if (nameExpression == null) {
            return CallTypeAndReceiver.UNKNOWN to null
        }

        val callTypeAndReceiver = CallTypeAndReceiver.detect(nameExpression)

        var receiverTypes = callTypeAndReceiver.receiverTypes(
                bindingContext, nameExpression, moduleDescriptor, resolutionFacade,
                predictableSmartCastsOnly = true /* we don't include smart cast receiver types for "unpredictable" receiver value to mark members grayed */)

        if (callTypeAndReceiver is CallTypeAndReceiver.SAFE) {
            receiverTypes = receiverTypes!!.map { it.makeNotNullable() }
        }

        return callTypeAndReceiver to receiverTypes
    }

    protected fun isImportableDescriptorImported(descriptor: DeclarationDescriptor): Boolean {
        val classification = importableFqNameClassifier.classify(descriptor.importableFqName!!, false)
        return classification != ImportableFqNameClassifier.Classification.notImported
               && classification != ImportableFqNameClassifier.Classification.hasImportFromSamePackage
    }
}
