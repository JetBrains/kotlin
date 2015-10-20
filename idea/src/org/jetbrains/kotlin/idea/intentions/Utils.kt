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

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KtType

fun KtCallableDeclaration.setType(type: KtType, shortenReferences: Boolean = true) {
    if (type.isError()) return
    setType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type), shortenReferences)
}

fun KtCallableDeclaration.setType(typeString: String, shortenReferences: Boolean = true) {
    val typeReference = KtPsiFactory(project).createType(typeString)
    setTypeReference(typeReference)
    if (shortenReferences) {
        ShortenReferences.DEFAULT.process(getTypeReference()!!)
    }
}

fun KtCallableDeclaration.setReceiverType(type: KtType) {
    if (type.isError()) return
    val typeReference = KtPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setReceiverTypeReference(typeReference)
    ShortenReferences.DEFAULT.process(getReceiverTypeReference()!!)
}

fun KtContainerNode.description(): String? {
    when (getNode().getElementType()) {
        KtNodeTypes.THEN -> return "if"
        KtNodeTypes.ELSE -> return "else"
        KtNodeTypes.BODY -> {
            when (getParent()) {
                is KtWhileExpression -> return "while"
                is KtDoWhileExpression -> return "do...while"
                is KtForExpression -> return "for"
            }
        }
    }
    return null
}

fun isAutoCreatedItUsage(expression: KtNameReferenceExpression): Boolean {
    if (expression.getReferencedName() != "it") return false
    val context = expression.analyze(BodyResolveMode.PARTIAL)
    val target = expression.mainReference.resolveToDescriptors(context).singleOrNull() as? ValueParameterDescriptor? ?: return false
    return context[BindingContext.AUTO_CREATED_IT, target]!!
}

// returns assignment which replaces initializer
fun splitPropertyDeclaration(property: KtProperty): KtBinaryExpression {
    val parent = property.getParent()!!

    val initializer = property.getInitializer()!!

    val explicitTypeToSet = if (property.getTypeReference() != null) null else initializer.analyze().getType(initializer)

    val psiFactory = KtPsiFactory(property)
    var assignment = psiFactory.createExpressionByPattern("$0 = $1", property.getNameAsName()!!, initializer)

    assignment = parent.addAfter(assignment, property) as KtBinaryExpression
    parent.addAfter(psiFactory.createNewLine(), property)

    property.setInitializer(null)

    if (explicitTypeToSet != null) {
        property.setType(explicitTypeToSet)
    }

    return assignment
}

val KtQualifiedExpression.callExpression: KtCallExpression?
    get() = getSelectorExpression() as? KtCallExpression

val KtQualifiedExpression.calleeName: String?
    get() = (callExpression?.getCalleeExpression() as? KtNameReferenceExpression)?.getText()

fun KtQualifiedExpression.toResolvedCall(): ResolvedCall<out CallableDescriptor>? {
    val callExpression = callExpression ?: return null
    return callExpression.getResolvedCall(callExpression.analyze()) ?: return null
}

fun KtExpression.isExitStatement(): Boolean {
    when (this) {
        is KtContinueExpression, is KtBreakExpression, is KtThrowExpression, is KtReturnExpression -> return true
        else -> return false
    }
}

// returns false for call of super, static method or method from package
fun KtQualifiedExpression.isReceiverExpressionWithValue(): Boolean {
    val receiver = getReceiverExpression()
    if (receiver is KtSuperExpression) return false
    return analyze().getType(receiver) != null
}

public fun KtExpression.negate(): KtExpression {
    val specialNegation = specialNegation()
    if (specialNegation != null) return specialNegation
    return KtPsiFactory(this).createExpressionByPattern("!$0", this)
}

private fun KtExpression.specialNegation(): KtExpression? {
    val factory = KtPsiFactory(this)
    when (this) {
        is KtPrefixExpression -> {
            if (getOperationReference().getReferencedName() == "!") {
                val baseExpression = getBaseExpression()
                if (baseExpression != null) {
                    return KtPsiUtil.safeDeparenthesize(baseExpression)
                }
            }
        }

        is KtBinaryExpression -> {
            val operator = getOperationToken()
            if (operator !in NEGATABLE_OPERATORS) return null
            val left = getLeft() ?: return null
            val right = getRight() ?: return null
            return factory.createExpressionByPattern("$0 $1 $2", left, getNegatedOperatorText(operator), right)
        }

        is KtConstantExpression -> {
            return when (getText()) {
                "true" -> factory.createExpression("false")
                "false" -> factory.createExpression("true")
                else -> null
            }
        }
    }
    return null
}

private val NEGATABLE_OPERATORS = setOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ,
                                        KtTokens.EXCLEQEQEQ, KtTokens.IS_KEYWORD, KtTokens.NOT_IS, KtTokens.IN_KEYWORD,
                                        KtTokens.NOT_IN, KtTokens.LT, KtTokens.LTEQ, KtTokens.GT, KtTokens.GTEQ)

private fun getNegatedOperatorText(token: IElementType): String {
    return when(token) {
        KtTokens.EQEQ -> KtTokens.EXCLEQ.getValue()
        KtTokens.EXCLEQ -> KtTokens.EQEQ.getValue()
        KtTokens.EQEQEQ -> KtTokens.EXCLEQEQEQ.getValue()
        KtTokens.EXCLEQEQEQ -> KtTokens.EQEQEQ.getValue()
        KtTokens.IS_KEYWORD -> KtTokens.NOT_IS.getValue()
        KtTokens.NOT_IS -> KtTokens.IS_KEYWORD.getValue()
        KtTokens.IN_KEYWORD -> KtTokens.NOT_IN.getValue()
        KtTokens.NOT_IN -> KtTokens.IN_KEYWORD.getValue()
        KtTokens.LT -> KtTokens.GTEQ.getValue()
        KtTokens.LTEQ -> KtTokens.GT.getValue()
        KtTokens.GT -> KtTokens.LTEQ.getValue()
        KtTokens.GTEQ -> KtTokens.LT.getValue()
        else -> throw IllegalArgumentException("The token $token does not have a negated equivalent.")
    }
}
