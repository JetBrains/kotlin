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
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.completion.smart.LambdaItems
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.comparePossiblyOverridingDescriptors
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.properties.Delegates

class CompletionSessionConfiguration(
        val completeNonImportedDeclarations: Boolean,
        val completeNonAccessibleDeclarations: Boolean)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
        completeNonImportedDeclarations = parameters.getInvocationCount() >= 2,
        completeNonAccessibleDeclarations = parameters.getInvocationCount() >= 2)

abstract class CompletionSessionBase(protected val configuration: CompletionSessionConfiguration,
                                     protected val parameters: CompletionParameters,
                                     resultSet: CompletionResultSet) {
    protected val position: PsiElement = parameters.getPosition()
    private val file = position.getContainingFile() as JetFile
    protected val resolutionFacade: ResolutionFacade = file.getResolutionFacade()
    protected val moduleDescriptor: ModuleDescriptor = resolutionFacade.findModuleDescriptor(file)
    protected val project: Project = position.getProject()

    protected val reference: JetSimpleNameReference?
    protected val expression: JetExpression?

    init {
        val reference = position.getParent()?.getReferences()?.firstIsInstanceOrNull<JetSimpleNameReference>()
        if (reference != null) {
            if (reference.expression is JetLabelReferenceExpression) {
                this.expression = reference.expression.getParent().getParent() as? JetExpressionWithLabel
                this.reference = null
            }
            else {
                this.expression = reference.expression
                this.reference = reference
            }
        }
        else {
            this.reference = null
            this.expression = null
        }
    }

    protected val bindingContext: BindingContext = resolutionFacade.analyze(position.parentsWithSelf.firstIsInstance<JetElement>(), BodyResolveMode.PARTIAL_FOR_COMPLETION)
    protected val inDescriptor: DeclarationDescriptor? = expression?.let { bindingContext.get(BindingContext.RESOLUTION_SCOPE, it)?.getContainingDeclaration() }

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

    protected val isVisibleFilter: (DeclarationDescriptor) -> Boolean = { isVisibleDescriptor(it) }

    protected val referenceVariantsHelper: ReferenceVariantsHelper = ReferenceVariantsHelper(bindingContext, moduleDescriptor, project, isVisibleFilter)

    protected val receiversData: ReferenceVariantsHelper.ReceiversData? = reference?.let { referenceVariantsHelper.getReferenceVariantsReceivers(it.expression) }

    protected val lookupElementFactory: LookupElementFactory = run {
        if (receiversData != null) {
            val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

            var receiverTypes = receiversData.receivers.flatMap {
                SmartCastUtils.getSmartCastVariantsWithLessSpecificExcluded(it, bindingContext, moduleDescriptor, dataFlowInfo)
            }

            if (receiversData.callType == CallType.SAFE) {
                receiverTypes = receiverTypes.map { it.makeNotNullable() }
            }

            LookupElementFactory(resolutionFacade, receiverTypes)
        }
        else {
            LookupElementFactory(resolutionFacade, emptyList())
        }
    }

    private val collectorContext = if (expression?.getParent() is JetSimpleNameStringTemplateEntry)
        LookupElementsCollector.Context.STRING_TEMPLATE_AFTER_DOLLAR
    else if (receiversData?.callType == CallType.INFIX)
        LookupElementsCollector.Context.INFIX_CALL
    else
        LookupElementsCollector.Context.NORMAL

    protected val collector: LookupElementsCollector = LookupElementsCollector(prefixMatcher, parameters, resultSet, resolutionFacade, lookupElementFactory, inDescriptor, collectorContext)

    protected val originalSearchScope: GlobalSearchScope = ResolutionFacade.getResolveScope(parameters.getOriginalFile() as JetFile)

    // we need to exclude the original file from scope because our resolve session is built with this file replaced by synthetic one
    protected val searchScope: GlobalSearchScope = object : DelegatingGlobalSearchScope(originalSearchScope) {
        override fun contains(file: VirtualFile) = super.contains(file) && file != parameters.getOriginalFile().getVirtualFile()
    }

    protected val indicesHelper: KotlinIndicesHelper
        get() = KotlinIndicesHelper(project, resolutionFacade, searchScope, moduleDescriptor, isVisibleFilter)

    protected fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is DeclarationDescriptorWithVisibility && inDescriptor != null) {
            val visible = descriptor.isVisible(inDescriptor, bindingContext, reference?.expression)
            if (visible) return true
            if (!configuration.completeNonAccessibleDeclarations) return false
            return DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) !is PsiCompiledElement
        }

        return true
    }

    protected fun flushToResultSet() {
        collector.flushToResultSet()
    }

    public fun complete(): Boolean {
        doComplete()
        flushToResultSet()
        return !collector.isResultEmpty
    }

    protected abstract fun doComplete()

    protected abstract val descriptorKindFilter: DescriptorKindFilter?

    // set is used only for completion in code fragments
    protected val referenceVariants: Collection<DeclarationDescriptor> by Delegates.lazy {
        if (descriptorKindFilter != null) {
            val expression = reference!!.expression
            referenceVariantsHelper.getReferenceVariants(expression, descriptorKindFilter!!, false, prefixMatcher.asNameFilter())
                    .excludeNonInitializedVariable(expression)
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
        val descriptors = referenceVariantsHelper.getReferenceVariants(reference!!.expression, descriptorKindFilter!!, true, prefixMatcher.asNameFilter())
        return descriptors.filter { descriptor ->
            referenceVariants.none { comparePossiblyOverridingDescriptors(project, it, descriptor) }
        }
    }

    protected fun shouldRunTopLevelCompletion(): Boolean
            = configuration.completeNonImportedDeclarations && isNoQualifierContext()

    protected fun isNoQualifierContext(): Boolean {
        val parent = position.getParent()
        return parent is JetSimpleNameExpression && !JetPsiUtil.isSelectorInQualified(parent)
    }

    protected fun getTopLevelCallables(): Collection<DeclarationDescriptor> {
        val descriptors = indicesHelper.getTopLevelCallables({ prefixMatcher.prefixMatches(it) })
        return filterShadowedNonImported(descriptors, reference!!)
    }

    protected fun getTopLevelExtensions(): Collection<CallableDescriptor> {
        val descriptors = indicesHelper.getCallableTopLevelExtensions({ prefixMatcher.prefixMatches(it) }, reference!!.expression, bindingContext)
        return filterShadowedNonImported(descriptors, reference)
    }

    private fun filterShadowedNonImported(descriptors: Collection<CallableDescriptor>, reference: JetSimpleNameReference): Collection<CallableDescriptor> {
        return ShadowedDeclarationsFilter(bindingContext, moduleDescriptor, project).filterNonImported(descriptors, referenceVariants, reference.expression)
    }

    protected fun addAllClasses(kindFilter: (ClassKind) -> Boolean) {
        AllClassesCompletion(parameters, indicesHelper, prefixMatcher, kindFilter)
                .collect(
                        { descriptor -> collector.addDescriptorElements(descriptor, suppressAutoInsertion = true) },
                        { javaClass -> collector.addElementWithAutoInsertionSuppressed(lookupElementFactory.createLookupElementForJavaClass(javaClass)) }
                )
    }
}

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    public enum class CompletionKind(val classKindFilter: ((ClassKind) -> Boolean)?) {
        KEYWORDS_ONLY(classKindFilter = null),
        NAMED_ARGUMENTS_ONLY(classKindFilter = null),
        ALL(classKindFilter = { it != ClassKind.ENUM_ENTRY }),
        TYPES(classKindFilter = { it != ClassKind.ENUM_ENTRY }),
        ANNOTATION_TYPES(classKindFilter = { it == ClassKind.ANNOTATION_CLASS }),
        ANNOTATION_TYPES_OR_PARAMETER_NAME(classKindFilter = { it == ClassKind.ANNOTATION_CLASS }),
        PARAMETER_NAME(classKindFilter = null)
    }

    public val completionKind: CompletionKind = calcCompletionKind()

    protected override val descriptorKindFilter = when (completionKind) {
        CompletionKind.TYPES ->
            DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude DescriptorKindExclude.EnumEntry

        CompletionKind.ANNOTATION_TYPES,  CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME ->
            DescriptorKindFilter(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude NonAnnotationClassifierExclude

        CompletionKind.ALL ->
            DescriptorKindFilter(DescriptorKindFilter.ALL_KINDS_MASK)

        CompletionKind.NAMED_ARGUMENTS_ONLY, CompletionKind.KEYWORDS_ONLY, CompletionKind.PARAMETER_NAME ->
            null
    }

    private val parameterNameAndTypeCompletion = if (shouldCompleteParameterNameAndType())
        ParameterNameAndTypeCompletion(collector, lookupElementFactory, prefixMatcher, resolutionFacade)
    else
        null

    private fun calcCompletionKind(): CompletionKind {
        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(position)) {
            return CompletionKind.NAMED_ARGUMENTS_ONLY
        }

        if (reference == null) {
            val parameter = position.getParent() as? JetParameter
            return if (parameter != null && position == parameter.getNameIdentifier())
                CompletionKind.PARAMETER_NAME
            else
                CompletionKind.KEYWORDS_ONLY
        }

        val annotationEntry = position.getStrictParentOfType<JetAnnotationEntry>()
        if (annotationEntry != null) {
            val valueArgList = position.getStrictParentOfType<JetValueArgumentList>()
            if (valueArgList == null || !annotationEntry.isAncestor(valueArgList)) {
                val parent = annotationEntry.getParent()
                if (parent is JetDeclarationModifierList && parent.getParent() is JetParameter) {
                    return CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME
                }
                return CompletionKind.ANNOTATION_TYPES
            }
        }

        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        val typeReference = position.getStrictParentOfType<JetTypeReference>()
        if (typeReference != null) {
            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<JetSimpleNameExpression>())
            if (firstPartReference == reference.expression) {
                return CompletionKind.TYPES
            }
        }

        return CompletionKind.ALL
    }

    private fun shouldCompleteParameterNameAndType(): Boolean {
        when (completionKind) {
            CompletionKind.PARAMETER_NAME, CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME -> {
                val parameter = position.getNonStrictParentOfType<JetParameter>()!!
                val owner = parameter.getParent().getParent()
                return parameter != (owner as? JetCatchClause)?.getCatchParameter() && parameter != (owner as? JetPropertyAccessor)?.getParameter()
            }

            else -> return false
        }
    }

    public fun shouldDisableAutoPopup(): Boolean {
        return when (completionKind) {
            CompletionKind.PARAMETER_NAME, CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME -> !shouldCompleteParameterNameAndType()
            else -> false
        }
    }

    override fun doComplete() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        // if we are typing parameter name, restart completion each time we type an upper case letter because new suggestions will appear (previous words can be used as user prefix)
        if (parameterNameAndTypeCompletion != null) {
            val prefixPattern = StandardPatterns.string().with(object : PatternCondition<String>("Prefix ends with uppercase letter") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix.isNotEmpty() && prefix.last().isUpperCase()
            })
            (CompletionService.getCompletionService().getCurrentCompletion() as CompletionProgressIndicator)
                    .addWatchedPrefix(parameters.getOffset(), prefixPattern)
        }

        if (completionKind == CompletionKind.PARAMETER_NAME || completionKind == CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME) {
            collector.suppressItemSelectionByCharsOnTyping = true
        }

        if (completionKind != CompletionKind.NAMED_ARGUMENTS_ONLY) {
            parameterNameAndTypeCompletion?.addFromParametersInFile(position, resolutionFacade, isVisibleFilter)
            flushToResultSet()

            parameterNameAndTypeCompletion?.addFromImportedClasses(position, bindingContext, isVisibleFilter)
            flushToResultSet()

            collector.addDescriptorElements(referenceVariants, suppressAutoInsertion = false)

            val keywordsPrefix = prefix.substringBefore('@') // if there is '@' in the prefix - use shorter prefix to not loose 'this' etc
            KeywordCompletion.complete(expression ?: parameters.getPosition(), keywordsPrefix) { lookupElement ->
                val keyword = lookupElement.getLookupString()
                when (keyword) {
                    // if "this" is parsed correctly in the current context - insert it and all this@xxx items
                    "this" -> {
                        if (expression != null) {
                            collector.addElements(thisExpressionItems(bindingContext, expression, prefix).map { it.factory() })
                        }
                        else {
                            // for completion in secondary constructor delegation call
                            collector.addElement(lookupElement)
                        }
                    }

                    // if "return" is parsed correctly in the current context - insert it and all return@xxx items
                    "return" -> {
                        if (expression != null) {
                            collector.addElements(returnExpressionItems(bindingContext, expression))
                        }
                    }

                    "break", "continue" -> {
                        if (expression != null) {
                            collector.addElements(breakOrContinueExpressionItems(expression, keyword))
                        }
                    }

                    else -> collector.addElement(lookupElement)
                }
            }

            if (completionKind != CompletionKind.KEYWORDS_ONLY) {
                flushToResultSet()

                if (!configuration.completeNonImportedDeclarations && isNoQualifierContext()) {
                    collector.advertiseSecondCompletion()
                }

                addNonImported(completionKind)

                if (position.getContainingFile() is JetCodeFragment) {
                    flushToResultSet()
                    collector.addDescriptorElements(getRuntimeReceiverTypeReferenceVariants(), suppressAutoInsertion = false, withReceiverCast = true)
                }
            }
        }

        NamedArgumentCompletion.complete(position, collector, bindingContext)
    }

    private object NonAnnotationClassifierExclude : DescriptorKindExclude {
        override fun matches(descriptor: DeclarationDescriptor): Boolean {
            return if (descriptor is ClassDescriptor)
                descriptor.getKind() != ClassKind.ANNOTATION_CLASS
            else
                descriptor !is ClassifierDescriptor
        }
    }

    private fun addNonImported(completionKind: CompletionKind) {
        if (completionKind == CompletionKind.ALL) {
            collector.addDescriptorElements(getTopLevelExtensions(), suppressAutoInsertion = true)
        }

        flushToResultSet()

        if (shouldRunTopLevelCompletion()) {
            completionKind.classKindFilter?.let { addAllClasses(it) }

            if (completionKind == CompletionKind.ALL) {
                collector.addDescriptorElements(getTopLevelCallables(), suppressAutoInsertion = true)
            }
        }

        parameterNameAndTypeCompletion?.addFromAllClasses(parameters, indicesHelper)
    }
}

class SmartCompletionSession(configuration: CompletionSessionConfiguration, parameters: CompletionParameters, resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    // we do not include SAM-constructors because they are handled separately and adding them requires iterating of java classes
    override val descriptorKindFilter: DescriptorKindFilter? = DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude

    override fun doComplete() {
        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(position)) {
            NamedArgumentCompletion.complete(position, collector, bindingContext)
            return
        }

        if (expression != null) {
            val mapper = ToFromOriginalFileMapper(parameters.getOriginalFile() as JetFile, position.getContainingFile() as JetFile, parameters.getOffset())

            addFunctionLiteralArgumentCompletions()

            val completion = SmartCompletion(expression, resolutionFacade, moduleDescriptor,
                                             bindingContext, isVisibleFilter, inDescriptor, prefixMatcher, originalSearchScope,
                                             mapper, lookupElementFactory)
            val result = completion.execute()
            if (result != null) {
                collector.addElements(result.additionalItems)

                if (reference != null) {
                    val filter = result.declarationFilter
                    if (filter != null) {
                        referenceVariants.forEach { collector.addElements(filter(it)) }
                        flushToResultSet()

                        processNonImported { collector.addElements(filter(it)) }
                        flushToResultSet()

                        if (position.getContainingFile() is JetCodeFragment) {
                            getRuntimeReceiverTypeReferenceVariants().forEach {
                                collector.addElements(filter(it).map { it.withReceiverCast() })
                            }
                            flushToResultSet()
                        }
                    }

                    // it makes no sense to search inheritors if there is no reference because it means that we have prefix like "this@"
                    result.inheritanceSearcher?.search({ prefixMatcher.prefixMatches(it) }) {
                        collector.addElement(it)
                        flushToResultSet()
                    }
                }
            }
        }
    }

    // special completion for outside parenthesis lambda argument
    private fun addFunctionLiteralArgumentCompletions() {
        if (reference != null) {
            val receiverData = ReferenceVariantsHelper.getExplicitReceiverData(reference.expression)
            if (receiverData != null && receiverData.second == CallType.INFIX) {
                val call = receiverData.first.getCall(bindingContext)
                if (call != null && call.getFunctionLiteralArguments().isEmpty()) {
                    val dummyArgument = object : FunctionLiteralArgument {
                        override fun getFunctionLiteral() = throw UnsupportedOperationException()
                        override fun getArgumentExpression() = throw UnsupportedOperationException()
                        override fun getArgumentName(): ValueArgumentName? = null
                        override fun isNamed() = false
                        override fun asElement() = throw UnsupportedOperationException()
                        override fun getSpreadElement(): LeafPsiElement? = null
                        override fun isExternal() = false
                    }
                    val dummyArguments = call.getValueArguments() + listOf(dummyArgument)
                    val dummyCall = object : DelegatingCall(call) {
                        override fun getValueArguments() = dummyArguments
                        override fun getFunctionLiteralArguments() = listOf(dummyArgument)
                        override fun getValueArgumentList() = throw UnsupportedOperationException()
                    }

                    val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade, moduleDescriptor, true)
                            .calculateForArgument(dummyCall, dummyArgument)
                    if (expectedInfos != null) {
                        collector.addElements(LambdaItems.collect(expectedInfos))
                    }
                }
            }
        }
    }

    private fun processNonImported(processor: (DeclarationDescriptor) -> Unit) {
        getTopLevelExtensions().forEach(processor)

        flushToResultSet()

        if (shouldRunTopLevelCompletion()) {
            getTopLevelCallables().forEach(processor)
        }
    }
}
