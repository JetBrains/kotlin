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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.refactoring.introduce.ExtractableSubstringInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractableSubstringInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.substringContextOrThis
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.hasBothReceivers
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

data class ExtractionOptions(
        val inferUnitTypeForUnusedValues: Boolean = true,
        val enableListBoxing: Boolean = false,
        val extractAsProperty: Boolean = false,
        val allowSpecialClassNames: Boolean = false,
        val captureLocalFunctions: Boolean = false,
        val canWrapInWith: Boolean = false
) {
    companion object {
        val DEFAULT = ExtractionOptions()
    }
}

data class ResolveResult(
        val originalRefExpr: KtSimpleNameExpression,
        val declaration: PsiElement,
        val descriptor: DeclarationDescriptor,
        val resolvedCall: ResolvedCall<*>?
)

data class ResolvedReferenceInfo(
        val refExpr: KtSimpleNameExpression,
        val resolveResult: ResolveResult,
        val smartCast: KotlinType?,
        val possibleTypes: Set<KotlinType>,
        val shouldSkipPrimaryReceiver: Boolean
)

internal var KtSimpleNameExpression.resolveResult: ResolveResult? by CopyableUserDataProperty(Key.create("RESOLVE_RESULT"))

data class ExtractionData(
        val originalFile: KtFile,
        val originalRange: KotlinPsiRange,
        val targetSibling: PsiElement,
        val duplicateContainer: PsiElement? = null,
        val options: ExtractionOptions = ExtractionOptions.DEFAULT
) : Disposable {
    val project: Project = originalFile.project
    val originalElements: List<PsiElement> = originalRange.elements
    val physicalElements = originalElements.map { it.substringContextOrThis }

    val substringInfo: ExtractableSubstringInfo?
        get() = (originalElements.singleOrNull() as? KtExpression)?.extractableSubstringInfo

    val insertBefore: Boolean = options.extractAsProperty
                                || targetSibling.getStrictParentOfType<KtDeclaration>()?.let {
                                    it is KtDeclarationWithBody || it is KtAnonymousInitializer
                                } ?: false

    val expressions = originalElements.filterIsInstance<KtExpression>()

    val codeFragmentText: String by lazy {
        val originalElements = originalElements
        when (originalElements.size) {
            0 -> ""
            1 -> originalElements.first().text
            else -> originalFile.text.substring(originalElements.first().startOffset, originalElements.last().endOffset)
        }
    }

    val commonParent = PsiTreeUtil.findCommonParent(physicalElements) as KtElement

    val bindingContext: BindingContext? by lazy { commonParent.analyze() }

    private val itFakeDeclaration by lazy { KtPsiFactory(originalFile).createParameter("it: Any?") }
    private val synthesizedInvokeDeclaration by lazy { KtPsiFactory(originalFile).createFunction("fun invoke() {}") }

    init {
        markReferences()
    }

    private fun isExtractableIt(descriptor: DeclarationDescriptor, context: BindingContext): Boolean {
        if (!(descriptor is ValueParameterDescriptor && (context[BindingContext.AUTO_CREATED_IT, descriptor] ?: false))) return false
        val function = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.containingDeclaration) as? KtFunctionLiteral
        return function == null || !function.isInsideOf(physicalElements)
    }

    private tailrec fun getDeclaration(descriptor: DeclarationDescriptor, context: BindingContext): PsiElement? {
        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
        if (declaration is PsiNameIdentifierOwner) {
            return declaration
        }

        return when {
            isExtractableIt(descriptor, context) -> itFakeDeclaration
            isSynthesizedInvoke(descriptor) -> synthesizedInvokeDeclaration
            descriptor is SyntheticJavaPropertyDescriptor -> getDeclaration(descriptor.getMethod, context)
            else -> declaration
        }
    }

    private fun markReferences() {
        val context = bindingContext ?: return
        val visitor = object : KtTreeVisitorVoid() {
            override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                if (context[BindingContext.SMARTCAST, expression] != null) {
                    expression.selectorExpression?.accept(this)
                    return
                }

                super.visitQualifiedExpression(expression)
            }

            override fun visitSimpleNameExpression(ref: KtSimpleNameExpression) {
                if (ref.parent is KtValueArgumentName) return

                val physicalRef = substringInfo?.let {
                    // If substring contains some references it must be extracted as a string template
                    val physicalExpression = expressions.single() as KtStringTemplateExpression
                    val extractedContentOffset = physicalExpression.getContentRange().startOffset + physicalExpression.startOffset
                    val offsetInExtracted = ref.startOffset - extractedContentOffset
                    val offsetInTemplate = it.relativeContentRange.startOffset + offsetInExtracted
                    it.template.findElementAt(offsetInTemplate)!!.getStrictParentOfType<KtSimpleNameExpression>()
                } ?: ref

                val resolvedCall = physicalRef.getResolvedCall(context)
                val descriptor = context[BindingContext.REFERENCE_TARGET, physicalRef] ?: return
                val declaration = getDeclaration(descriptor, context) ?: return

                val resolveResult = ResolveResult(physicalRef, declaration, descriptor, resolvedCall)
                physicalRef.resolveResult = resolveResult
                if (ref != physicalRef) {
                    ref.resolveResult = resolveResult
                }
            }
        }
        expressions.forEach { it.accept(visitor) }
    }

    fun getPossibleTypes(expression: KtExpression, resolvedCall: ResolvedCall<*>?, context: BindingContext): Set<KotlinType> {
        val dataFlowInfo = context.getDataFlowInfoAfter(expression)

        resolvedCall?.getImplicitReceiverValue()?.let {
            return dataFlowInfo.getCollectedTypes(DataFlowValueFactory.createDataFlowValueForStableReceiver(it))
        }

        val type = resolvedCall?.resultingDescriptor?.returnType ?: return emptySet()
        val containingDescriptor = expression.getResolutionScope(context, expression.getResolutionFacade()).ownerDescriptor
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, context, containingDescriptor)
        return dataFlowInfo.getCollectedTypes(dataFlowValue)
    }

    fun getBrokenReferencesInfo(body: KtBlockExpression): List<ResolvedReferenceInfo> {
        val originalContext = bindingContext ?: return listOf()

        val newReferences = body.collectDescendantsOfType<KtSimpleNameExpression> { it.resolveResult != null }

        val context = body.analyze()

        val referencesInfo = ArrayList<ResolvedReferenceInfo>()
        for (newRef in newReferences) {
            val originalResolveResult = newRef.resolveResult ?: continue

            val smartCast: KotlinType?
            val possibleTypes: Set<KotlinType>
            val shouldSkipPrimaryReceiver: Boolean

            // Qualified property reference: a.b
            val qualifiedExpression = newRef.getQualifiedExpressionForSelector()
            if (qualifiedExpression != null) {
                val smartCastTarget = originalResolveResult.originalRefExpr.parent as KtExpression
                smartCast = originalContext[BindingContext.SMARTCAST, smartCastTarget]?.defaultType
                possibleTypes = getPossibleTypes(smartCastTarget, originalResolveResult.resolvedCall, originalContext)
                val receiverDescriptor =
                        (originalResolveResult.resolvedCall?.dispatchReceiver as? ImplicitReceiver)?.declarationDescriptor
                shouldSkipPrimaryReceiver = smartCast == null
                                            && !DescriptorUtils.isCompanionObject(receiverDescriptor)
                                            && qualifiedExpression.receiverExpression !is KtSuperExpression
                if (shouldSkipPrimaryReceiver && !(originalResolveResult.resolvedCall?.hasBothReceivers() ?: false)) continue
            }
            else {
                if (newRef.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } != null) continue
                smartCast = originalContext[BindingContext.SMARTCAST, originalResolveResult.originalRefExpr]?.defaultType
                possibleTypes = getPossibleTypes(originalResolveResult.originalRefExpr, originalResolveResult.resolvedCall, originalContext)
                shouldSkipPrimaryReceiver = false
            }

            val parent = newRef.parent

            // Skip P in type references like 'P.Q'
            if (parent is KtUserType && (parent.parent as? KtUserType)?.qualifier == parent) continue

            val descriptor = context[BindingContext.REFERENCE_TARGET, newRef]
            val isBadRef = !(compareDescriptors(project, originalResolveResult.descriptor, descriptor)
                             && originalContext.diagnostics.forElement(originalResolveResult.originalRefExpr) == context.diagnostics.forElement(newRef))
                           || smartCast != null
            if (isBadRef && !originalResolveResult.declaration.isInsideOf(physicalElements)) {
                val originalResolvedCall = originalResolveResult.resolvedCall as? VariableAsFunctionResolvedCall
                val originalFunctionCall = originalResolvedCall?.functionCall
                val originalVariableCall = originalResolvedCall?.variableCall
                val invokeDescriptor = originalFunctionCall?.resultingDescriptor
                if (invokeDescriptor != null) {
                    val invokeDeclaration = getDeclaration(invokeDescriptor, context) ?: synthesizedInvokeDeclaration
                    val variableResolveResult = originalResolveResult.copy(resolvedCall = originalVariableCall!!,
                                                                           descriptor = originalVariableCall.resultingDescriptor)
                    val functionResolveResult = originalResolveResult.copy(resolvedCall = originalFunctionCall,
                                                                           descriptor = originalFunctionCall.resultingDescriptor,
                                                                           declaration = invokeDeclaration)
                    referencesInfo.add(ResolvedReferenceInfo(newRef, variableResolveResult, smartCast, possibleTypes, shouldSkipPrimaryReceiver))
                    referencesInfo.add(ResolvedReferenceInfo(newRef, functionResolveResult, smartCast, possibleTypes, shouldSkipPrimaryReceiver))
                }
                else {
                    referencesInfo.add(ResolvedReferenceInfo(newRef, originalResolveResult, smartCast, possibleTypes, shouldSkipPrimaryReceiver))
                }
            }
        }

        return referencesInfo
    }

    override fun dispose() {
        expressions.forEach(::unmarkReferencesInside)
    }
}

fun unmarkReferencesInside(root: PsiElement) {
    if (!root.isValid) return
    root.forEachDescendantOfType<KtSimpleNameExpression> { it.resolveResult = null }
}