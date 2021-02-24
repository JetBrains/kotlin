/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.inspections.RedundantSamConstructorInspection
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType

@Suppress("DEPRECATION")
class ObjectLiteralToLambdaInspection : IntentionBasedInspection<KtObjectLiteralExpression>(ObjectLiteralToLambdaIntention::class) {
    override fun problemHighlightType(element: KtObjectLiteralExpression): ProblemHighlightType {
        val (_, baseType, singleFunction) = extractData(element) ?: return super.problemHighlightType(element)
        val bodyBlock = singleFunction.bodyBlockExpression
        val lastStatement = bodyBlock?.statements?.lastOrNull()
        if (bodyBlock?.anyDescendantOfType<KtReturnExpression> { it != lastStatement } == true) return ProblemHighlightType.INFORMATION

        val valueArgument = element.parent as? KtValueArgument
        val call = valueArgument?.getStrictParentOfType<KtCallExpression>()
        if (call != null) {
            val argumentMatch = call.resolveToCall()?.getArgumentMapping(valueArgument) as? ArgumentMatch
            if (baseType.constructor != argumentMatch?.valueParameter?.type?.constructor) return ProblemHighlightType.INFORMATION
        }

        return super.problemHighlightType(element)
    }
}

class ObjectLiteralToLambdaIntention : SelfTargetingRangeIntention<KtObjectLiteralExpression>(
    KtObjectLiteralExpression::class.java,
    KotlinBundle.lazyMessage("convert.to.lambda"),
    KotlinBundle.lazyMessage("convert.object.literal.to.lambda")
) {
    override fun applicabilityRange(element: KtObjectLiteralExpression): TextRange? {
        val (baseTypeRef, baseType, singleFunction) = extractData(element) ?: return null

        if (!JavaSingleAbstractMethodUtils.isSamType(baseType)) return null

        val functionDescriptor = singleFunction.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return null
        val overridden = functionDescriptor.overriddenDescriptors.singleOrNull() ?: return null
        if (overridden.modality != Modality.ABSTRACT) return null

        if (!singleFunction.hasBody()) return null
        if (singleFunction.valueParameters.any { it.name == null }) return null

        val bodyExpression = singleFunction.bodyExpression!!
        val context = bodyExpression.analyze()
        val containingDeclaration = functionDescriptor.containingDeclaration

        // this-reference
        if (bodyExpression.anyDescendantOfType<KtThisExpression> { thisReference ->
                context[BindingContext.REFERENCE_TARGET, thisReference.instanceReference] == containingDeclaration
            }
        ) return null

        // Recursive call, skip labels
        if (ReferencesSearch.search(singleFunction, LocalSearchScope(bodyExpression)).any { it.element !is KtLabelReferenceExpression }) {
            return null
        }

        fun ReceiverValue?.isImplicitClassFor(descriptor: DeclarationDescriptor) =
            this is ImplicitClassReceiver && classDescriptor == descriptor

        if (bodyExpression.anyDescendantOfType<KtExpression> { expression ->
                val resolvedCall = expression.getResolvedCall(context)
                resolvedCall?.let {
                    it.dispatchReceiver.isImplicitClassFor(containingDeclaration) || it.extensionReceiver
                        .isImplicitClassFor(containingDeclaration)
                } == true
            }
        ) return null

        return TextRange(element.objectDeclaration.getObjectKeyword()!!.startOffset, baseTypeRef.endOffset)
    }

    override fun applyTo(element: KtObjectLiteralExpression, editor: Editor?) {

        val (_, baseType, singleFunction) = extractData(element)!!

        val commentSaver = CommentSaver(element)
        val returnSaver = ReturnSaver(singleFunction)

        val body = singleFunction.bodyExpression!!

        val factory = KtPsiFactory(element)
        val newExpression = factory.buildExpression {
            appendFixedText(IdeDescriptorRenderers.SOURCE_CODE.renderType(baseType))

            appendFixedText("{")

            val parameters = singleFunction.valueParameters

            val needParameters =
                parameters.count() > 1 || parameters.any { parameter -> ReferencesSearch.search(parameter, LocalSearchScope(body)).any() }
            if (needParameters) {
                parameters.forEachIndexed { index, parameter ->
                    if (index > 0) {
                        appendFixedText(",")
                    }
                    appendName(parameter.nameAsSafeName)
                }

                appendFixedText("->")
            }

            val lastCommentOwner = if (singleFunction.hasBlockBody()) {
                val contentRange = (body as KtBlockExpression).contentRange()
                appendChildRange(contentRange)
                contentRange.last
            } else {
                appendExpression(body)
                body
            }

            if (lastCommentOwner?.anyDescendantOfType<PsiComment> { it.tokenType == KtTokens.EOL_COMMENT } == true) {
                appendFixedText("\n")
            }
            appendFixedText("}")
        }

        val replaced = runWriteAction { element.replaced(newExpression) }
        val pointerToReplaced = replaced.createSmartPointer()
        val callee = replaced.callee
        val callExpression = callee.parent as KtCallExpression
        val functionLiteral = callExpression.lambdaArguments.single().getLambdaExpression()!!

        val returnLabel = callee.getReferencedNameAsName()
        runWriteAction {
            returnSaver.restore(functionLiteral, returnLabel)
        }
        val parentCall = ((replaced.parent as? KtValueArgument)
            ?.parent as? KtValueArgumentList)
            ?.parent as? KtCallExpression
        if (parentCall != null && RedundantSamConstructorInspection.samConstructorCallsToBeConverted(parentCall)
                .singleOrNull() == callExpression
        ) {
            runWriteAction {
                commentSaver.restore(replaced, forceAdjustIndent = true/* by some reason lambda body is sometimes not properly indented */)
            }
            RedundantSamConstructorInspection.replaceSamConstructorCall(callExpression)
            if (parentCall.canMoveLambdaOutsideParentheses()) runWriteAction {
                parentCall.moveFunctionLiteralOutsideParentheses()
            }
        } else {
            runWriteAction {
                commentSaver.restore(replaced, forceAdjustIndent = true/* by some reason lambda body is sometimes not properly indented */)
            }
            pointerToReplaced.element?.let { replacedByPointer ->
                val endOffset = (replacedByPointer.callee.parent as? KtCallExpression)?.typeArgumentList?.endOffset
                    ?: replacedByPointer.callee.endOffset
                ShortenReferences.DEFAULT.process(replacedByPointer.containingKtFile, replacedByPointer.startOffset, endOffset)
            }
        }
    }

    private val KtExpression.callee
        get() = getCalleeExpressionIfAny() as KtNameReferenceExpression
}

private data class Data(
    val baseTypeRef: KtTypeReference,
    val baseType: KotlinType,
    val singleFunction: KtNamedFunction
)

private fun extractData(element: KtObjectLiteralExpression): Data? {
    val objectDeclaration = element.objectDeclaration

    val singleFunction = objectDeclaration.declarations.singleOrNull() as? KtNamedFunction ?: return null
    if (!singleFunction.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null

    val delegationSpecifier = objectDeclaration.superTypeListEntries.singleOrNull() ?: return null
    val typeRef = delegationSpecifier.typeReference ?: return null
    val bindingContext = typeRef.analyze(BodyResolveMode.PARTIAL)
    val baseType = bindingContext[BindingContext.TYPE, typeRef] ?: return null

    return Data(typeRef, baseType, singleFunction)
}