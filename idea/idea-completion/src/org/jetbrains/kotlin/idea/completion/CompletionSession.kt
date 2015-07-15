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
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
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

abstract class CompletionSession(protected val configuration: CompletionSessionConfiguration,
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
        get() = KotlinIndicesHelper(project, resolutionFacade, searchScope, moduleDescriptor, isVisibleFilter, true)

    protected fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val visible = descriptor.isVisible(inDescriptor, bindingContext, reference?.expression)
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
