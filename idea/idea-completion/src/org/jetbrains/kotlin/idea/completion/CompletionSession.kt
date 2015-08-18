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

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class CompletionSessionConfiguration(
        val completeNonImportedDeclarations: Boolean,
        val completeNonAccessibleDeclarations: Boolean,
        val filterOutJavaGettersAndSetters: Boolean
)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
        completeNonImportedDeclarations = parameters.getInvocationCount() >= 2,
        completeNonAccessibleDeclarations = parameters.getInvocationCount() >= 2,
        filterOutJavaGettersAndSetters = parameters.getInvocationCount() < 2
)

abstract class CompletionSession(protected val configuration: CompletionSessionConfiguration,
                                     protected val parameters: CompletionParameters,
                                     resultSet: CompletionResultSet) {
    protected val position: PsiElement = parameters.getPosition()
    private val file = position.getContainingFile() as JetFile
    protected val resolutionFacade: ResolutionFacade = file.getResolutionFacade()
    protected val moduleDescriptor: ModuleDescriptor = resolutionFacade.moduleDescriptor
    protected val project: Project = position.getProject()

    protected val nameExpression: JetSimpleNameExpression?
    protected val expression: JetExpression?

    init {
        val reference = (position.getParent() as? JetSimpleNameExpression)?.mainReference
        if (reference != null) {
            if (reference.expression is JetLabelReferenceExpression) {
                this.nameExpression = null
                this.expression = reference.expression.getParent().getParent() as? JetExpressionWithLabel
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

    protected val bindingContext: BindingContext = resolutionFacade.analyze(position.parentsWithSelf.firstIsInstance<JetElement>(), BodyResolveMode.PARTIAL_FOR_COMPLETION)
    protected val inDescriptor: DeclarationDescriptor = position.getResolutionScope(bindingContext, resolutionFacade).getContainingDeclaration()

    private val kotlinIdentifierStartPattern: ElementPattern<Char>
    private val kotlinIdentifierPartPattern: ElementPattern<Char>

    init {
        val includeDollar = position.prevLeaf()?.getNode()?.getElementType() != JetTokens.SHORT_TEMPLATE_ENTRY_START
        if (includeDollar) {
            kotlinIdentifierStartPattern = StandardPatterns.character().javaIdentifierStart()
            kotlinIdentifierPartPattern = StandardPatterns.character().javaIdentifierPart()
        }
        else {
            kotlinIdentifierStartPattern = StandardPatterns.character().javaIdentifierStart() andNot singleCharPattern('$')
            kotlinIdentifierPartPattern = StandardPatterns.character().javaIdentifierPart() andNot singleCharPattern('$')
        }
    }

    protected val prefix: String = CompletionUtil.findIdentifierPrefix(
            parameters.getPosition().getContainingFile(),
            parameters.getOffset(),
            kotlinIdentifierPartPattern or singleCharPattern('@'),
            kotlinIdentifierStartPattern)

    protected val prefixMatcher: PrefixMatcher = CamelHumpMatcher(prefix)

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

    protected val referenceVariantsHelper: ReferenceVariantsHelper = ReferenceVariantsHelper(bindingContext, resolutionFacade, isVisibleFilter)

    protected val receiversData: ReferenceVariantsHelper.ReceiversData? = nameExpression?.let { referenceVariantsHelper.getReferenceVariantsReceivers(it) }

    protected val lookupElementFactory: LookupElementFactory = run {
        var receiverTypes = emptyList<JetType>()
        if (receiversData != null) {
            val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

            receiverTypes = receiversData.receivers.flatMap { receiverValue ->
                val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, bindingContext, moduleDescriptor)
                if (dataFlowValue.isPredictable) { // we don't include smart cast receiver types for "unpredictable" receiver value to mark members grayed
                    resolutionFacade.frontendService<SmartCastManager>()
                            .getSmartCastVariantsWithLessSpecificExcluded(receiverValue, bindingContext, moduleDescriptor, dataFlowInfo)
                }
                else {
                    listOf(receiverValue.type)
                }
            }

            if (receiversData.callType == CallType.SAFE) {
                receiverTypes = receiverTypes.map { it.makeNotNullable() }
            }
        }

        LookupElementFactory(resolutionFacade, receiverTypes, { expectedInfos })
    }

    private val collectorContext = if (expression?.getParent() is JetSimpleNameStringTemplateEntry)
        LookupElementsCollector.Context.STRING_TEMPLATE_AFTER_DOLLAR
    else if (receiversData?.callType == CallType.INFIX)
        LookupElementsCollector.Context.INFIX_CALL
    else
        LookupElementsCollector.Context.NORMAL

    // LookupElementsCollector instantiation is deferred because virtual call to createSorter uses data from derived classes
    protected val collector: LookupElementsCollector by lazy {
        LookupElementsCollector(prefixMatcher, parameters, resultSet, resolutionFacade, lookupElementFactory, createSorter(), inDescriptor, collectorContext)
    }

    protected val originalSearchScope: GlobalSearchScope = getResolveScope(parameters.getOriginalFile() as JetFile)

    // we need to exclude the original file from scope because our resolve session is built with this file replaced by synthetic one
    protected val searchScope: GlobalSearchScope = object : DelegatingGlobalSearchScope(originalSearchScope) {
        override fun contains(file: VirtualFile) = super.contains(file) && file != parameters.getOriginalFile().getVirtualFile()
    }

    protected val indicesHelper: KotlinIndicesHelper
        get() = KotlinIndicesHelper(resolutionFacade, searchScope, isVisibleFilter, true)

    protected val toFromOriginalFileMapper: ToFromOriginalFileMapper
            = ToFromOriginalFileMapper(parameters.originalFile as JetFile, position.containingFile as JetFile, parameters.offset)

    protected fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
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

    protected open fun createSorter(): CompletionSorter {
        var sorter = CompletionSorter.defaultSorter(parameters, prefixMatcher)!!

        val importableFqNameClassifier = ImportableFqNameClassifier(file)

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

    protected val referenceVariants: Collection<DeclarationDescriptor> by lazy {
        if (descriptorKindFilter != null) {
            referenceVariantsHelper.getReferenceVariants(
                    nameExpression!!,
                    descriptorKindFilter!!,
                    descriptorNameFilter,
                    filterOutJavaGettersAndSetters = configuration.filterOutJavaGettersAndSetters
            ).excludeNonInitializedVariable(nameExpression)
        }
        else {
            emptyList()
        }
    }

    // filters out variable inside its initializer
    private fun Collection<DeclarationDescriptor>.excludeNonInitializedVariable(expression: JetExpression): Collection<DeclarationDescriptor> {
        for (element in expression.parentsWithSelf) {
            val parent = element.getParent()
            if (parent is JetVariableDeclaration && element == parent.getInitializer()) {
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent]
                return this.filter { it != descriptor }
            }
            if (element is JetDeclaration) break // we can use variable inside lambda or anonymous object located in its initializer
        }
        return this
    }

    protected fun getRuntimeReceiverTypeReferenceVariants(): Collection<DeclarationDescriptor> {
        val restrictedKindFilter = descriptorKindFilter!!.restrictedToKinds(DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.VARIABLES_MASK) // optimization
        val descriptors = referenceVariantsHelper.getReferenceVariants(nameExpression!!, restrictedKindFilter, descriptorNameFilter, useRuntimeReceiverType = true)
        return descriptors.filter { descriptor ->
            referenceVariants.filter { it.name == descriptor.name }.none { comparePossiblyOverridingDescriptors(project, it, descriptor) }
        }
    }

    protected fun shouldRunTopLevelCompletion(): Boolean
            = configuration.completeNonImportedDeclarations && isNoQualifierContext()

    protected fun isNoQualifierContext(): Boolean {
        val parent = position.getParent()
        return parent is JetSimpleNameExpression && !JetPsiUtil.isSelectorInQualified(parent)
    }

    protected fun getTopLevelCallables(): Collection<DeclarationDescriptor> {
        return indicesHelper.getTopLevelCallables({ prefixMatcher.prefixMatches(it) })
                .filterShadowedNonImported()
    }

    protected fun getTopLevelExtensions(): Collection<CallableDescriptor> {
        return indicesHelper.getCallableTopLevelExtensions({ prefixMatcher.prefixMatches(it) }, nameExpression!!, bindingContext)
                .filterShadowedNonImported()
    }

    private fun Collection<CallableDescriptor>.filterShadowedNonImported(): Collection<CallableDescriptor> {
        return ShadowedDeclarationsFilter(bindingContext, resolutionFacade).filterNonImported(this, referenceVariants, nameExpression!!)
    }

    protected fun addAllClasses(kindFilter: (ClassKind) -> Boolean) {
        AllClassesCompletion(parameters, indicesHelper, prefixMatcher, kindFilter)
                .collect(
                        { descriptor -> collector.addDescriptorElements(descriptor, notImported = true) },
                        { javaClass -> collector.addElement(lookupElementFactory.createLookupElementForJavaClass(javaClass), notImported = true) }
                )
    }
}
