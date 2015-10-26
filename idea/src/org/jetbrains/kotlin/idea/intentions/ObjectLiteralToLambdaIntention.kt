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
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.inspections.RedundantSamConstructorInspection
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ObjectLiteralToLambdaInspection : IntentionBasedInspection<KtObjectLiteralExpression>(ObjectLiteralToLambdaIntention())

class ObjectLiteralToLambdaIntention : JetSelfTargetingRangeIntention<KtObjectLiteralExpression>(
        KtObjectLiteralExpression::class.java,
        "Convert to lambda",
        "Convert object literal to lambda"
) {
    override fun applicabilityRange(element: KtObjectLiteralExpression): TextRange? {
        val (baseTypeRef, baseType, singleFunction) = extractData(element) ?: return null

        if (!SingleAbstractMethodUtils.isSamType(baseType)) return null

        val functionDescriptor = singleFunction.resolveToDescriptor() as? FunctionDescriptor ?: return null
        val overridden = functionDescriptor.overriddenDescriptors.singleOrNull() ?: return null
        if (overridden.modality != Modality.ABSTRACT) return null

        if (!singleFunction.hasBody()) return null
        if (singleFunction.valueParameters.any { it.name == null }) return null

        return TextRange(element.objectDeclaration.getObjectKeyword().startOffset, baseTypeRef.endOffset)
    }

    override fun applyTo(element: KtObjectLiteralExpression, editor: Editor) {
        applyTo(element)
    }

    fun applyTo(element: KtObjectLiteralExpression) {
        val commentSaver = CommentSaver(element)

        val (@Suppress("UNUSED_VARIABLE") baseTypeRef, baseType, singleFunction) = extractData(element)!!

        val RETURN_KEY = Key<Unit>("RETURN_KEY")

        val body = singleFunction.bodyExpression!!
        body.forEachDescendantOfType<KtReturnExpression> {
            if (it.getTargetFunction(it.analyze(BodyResolveMode.PARTIAL)) == singleFunction) {
                it.putCopyableUserData(RETURN_KEY, Unit)
            }
        }

        val factory = KtPsiFactory(element)
        val newExpression = factory.buildExpression {
            appendFixedText(IdeDescriptorRenderers.SOURCE_CODE.renderType(baseType))

            appendFixedText("{")

            val parameters = singleFunction.valueParameters

            val needParameters = parameters.any { parameter -> ReferencesSearch.search(parameter, LocalSearchScope(body)).any() }
            if (needParameters) {
                parameters.forEachIndexed { index, parameter ->
                    if (index > 0) {
                        appendFixedText(",")
                    }
                    appendName(parameter.nameAsSafeName)
                }

                appendFixedText("->")
            }

            if (singleFunction.hasBlockBody()) {
                appendChildRange((body as KtBlockExpression).contentRange())
            }
            else {
                appendExpression(body)
            }

            appendFixedText("}")
        }

        body.forEachDescendantOfType<KtReturnExpression> { it.putCopyableUserData(RETURN_KEY, null) }

        val replaced = element.replaced(newExpression)
        commentSaver.restore(replaced, forceAdjustIndent = true/* by some reason lambda body is sometimes not properly indented */)

        val callee = replaced.getCalleeExpressionIfAny()!! as KtNameReferenceExpression
        val callExpression = callee.parent as KtCallExpression
        val functionLiteral = callExpression.functionLiteralArguments.single().getFunctionLiteral()

        val lambdaBody = functionLiteral.bodyExpression!!

        val returnToReplace = functionLiteral.collectDescendantsOfType<KtReturnExpression>() { it.getCopyableUserData(RETURN_KEY) != null }

        val returnLabel = callee.getReferencedNameAsName()
        for (returnExpression in returnToReplace) {
            val value = returnExpression.returnedExpression
            val replaceWith = if (value != null && returnExpression.isValueOfBlock(lambdaBody)) {
                value
            }
            else if (value != null) {
                factory.createExpressionByPattern("return@$0 $1", returnLabel, value)
            }
            else {
                factory.createExpressionByPattern("return@$0", returnLabel)
            }

            returnExpression.replace(replaceWith)

        }

        val parentCall = ((replaced.parent as? KtValueArgument)
                             ?.parent as? KtValueArgumentList)
                                 ?.parent as? KtCallExpression
        if (parentCall != null && RedundantSamConstructorInspection.samConstructorCallsToBeConverted(parentCall).singleOrNull() == callExpression) {
            RedundantSamConstructorInspection.replaceSamConstructorCall(callExpression)
        }
        else {
            ShortenReferences.DEFAULT.process(replaced.getContainingJetFile(), replaced.startOffset, callee.endOffset)
        }
    }

    private fun KtExpression.isValueOfBlock(inBlock: KtBlockExpression): Boolean {
        val parent = parent
        when (parent) {
            inBlock -> {
                return this == inBlock.statements.last()
            }

            is KtBlockExpression -> {
                return isValueOfBlock(parent) && parent.isValueOfBlock(inBlock)
            }

            is KtContainerNode -> {
                val owner = parent.parent
                if (owner is KtIfExpression) {
                    return (this == owner.then || this == owner.`else`) && owner.isValueOfBlock(inBlock)
                }
            }

            is KtWhenEntry -> {
                return this == parent.expression && (parent.parent as KtWhenExpression).isValueOfBlock(inBlock)
            }
        }

        return false
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

        val delegationSpecifier = objectDeclaration.getDelegationSpecifiers().singleOrNull() ?: return null
        val typeRef = delegationSpecifier.typeReference ?: return null
        val bindingContext = typeRef.analyze(BodyResolveMode.PARTIAL)
        val baseType = bindingContext[BindingContext.TYPE, typeRef] ?: return null

        return Data(typeRef, baseType, singleFunction)
    }
}