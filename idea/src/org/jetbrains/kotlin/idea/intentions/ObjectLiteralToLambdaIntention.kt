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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.inspections.RedundantSamConstructorInspection
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ObjectLiteralToLambdaInspection : IntentionBasedInspection<KtObjectLiteralExpression>(ObjectLiteralToLambdaIntention::class)

class ObjectLiteralToLambdaIntention : SelfTargetingRangeIntention<KtObjectLiteralExpression>(
        KtObjectLiteralExpression::class.java,
        "Convert to lambda",
        "Convert object literal to lambda"
) {
    override fun applicabilityRange(element: KtObjectLiteralExpression): TextRange? {
        val (baseTypeRef, baseType, singleFunction) = extractData(element) ?: return null

        if (!SingleAbstractMethodUtils.isSamType(baseType)) return null

        val functionDescriptor = singleFunction.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? FunctionDescriptor ?: return null
        val overridden = functionDescriptor.overriddenDescriptors.singleOrNull() ?: return null
        if (overridden.modality != Modality.ABSTRACT) return null

        if (!singleFunction.hasBody()) return null
        if (singleFunction.valueParameters.any { it.name == null }) return null

        val bodyExpression = singleFunction.bodyExpression!!

        // this-reference
        val thisReferences = bodyExpression.collectDescendantsOfType<KtThisExpression> { it !is KtClassOrObject }
        for (thisReference in thisReferences) {
            val context = thisReference.analyze(BodyResolveMode.PARTIAL)
            val thisDescriptor = context[BindingContext.REFERENCE_TARGET, thisReference.instanceReference]
            if (thisDescriptor == functionDescriptor.containingDeclaration) {
                return null
            }
        }

        // Recursive call, skip labels
        if (ReferencesSearch.search(singleFunction, LocalSearchScope(bodyExpression)).any { it.element !is KtLabelReferenceExpression }) {
            return null
        }

        return TextRange(element.objectDeclaration.getObjectKeyword()!!.startOffset, baseTypeRef.endOffset)
    }

    override fun applyTo(element: KtObjectLiteralExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element)

        val (_, baseType, singleFunction) = extractData(element)!!

        val returnSaver = ReturnSaver(singleFunction)

        val body = singleFunction.bodyExpression!!

        val factory = KtPsiFactory(element)
        val newExpression = factory.buildExpression {
            appendFixedText(IdeDescriptorRenderers.SOURCE_CODE.renderType(baseType))

            appendFixedText("{")

            val parameters = singleFunction.valueParameters

            val needParameters = parameters.count() > 1 || parameters.any { parameter -> ReferencesSearch.search(parameter, LocalSearchScope(body)).any() }
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
            }
            else {
                appendExpression(body)
                body
            }

            if (lastCommentOwner?.anyDescendantOfType<PsiComment> { it.tokenType == KtTokens.EOL_COMMENT } ?: false) {
                appendFixedText("\n")
            }
            appendFixedText("}")
        }

        val replaced = element.replaced(newExpression)
        commentSaver.restore(replaced, forceAdjustIndent = true/* by some reason lambda body is sometimes not properly indented */)

        val callee = replaced.getCalleeExpressionIfAny()!! as KtNameReferenceExpression
        val callExpression = callee.parent as KtCallExpression
        val functionLiteral = callExpression.lambdaArguments.single().getLambdaExpression()

        val returnLabel = callee.getReferencedNameAsName()
        returnSaver.restore(functionLiteral, returnLabel)

        val parentCall = ((replaced.parent as? KtValueArgument)
                             ?.parent as? KtValueArgumentList)
                                 ?.parent as? KtCallExpression
        if (parentCall != null && RedundantSamConstructorInspection.samConstructorCallsToBeConverted(parentCall).singleOrNull() == callExpression) {
            RedundantSamConstructorInspection.replaceSamConstructorCall(callExpression)
            if (MoveLambdaOutsideParenthesesIntention.canMove(parentCall)) {
                parentCall.moveFunctionLiteralOutsideParentheses()
            }
        }
        else {
            val endOffset = (callee.parent as? KtCallExpression)?.typeArgumentList?.endOffset ?: callee.endOffset
            ShortenReferences.DEFAULT.process(replaced.getContainingKtFile(), replaced.startOffset, endOffset)
        }
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

        val delegationSpecifier = objectDeclaration.getSuperTypeListEntries().singleOrNull() ?: return null
        val typeRef = delegationSpecifier.typeReference ?: return null
        val bindingContext = typeRef.analyze(BodyResolveMode.PARTIAL)
        val baseType = bindingContext[BindingContext.TYPE, typeRef] ?: return null

        return Data(typeRef, baseType, singleFunction)
    }
}
