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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isFlexible
import java.lang.IllegalArgumentException

fun KtContainerNode.description(): String? {
    when (node.elementType) {
        KtNodeTypes.THEN -> return "if"
        KtNodeTypes.ELSE -> return "else"
        KtNodeTypes.BODY -> {
            when (parent) {
                is KtWhileExpression -> return "while"
                is KtDoWhileExpression -> return "do...while"
                is KtForExpression -> return "for"
            }
        }
    }
    return null
}

fun KtCallExpression.isMethodCall(fqMethodName: String): Boolean {
    val resolvedCall = this.getResolvedCall(this.analyze()) ?: return false
    return resolvedCall.resultingDescriptor.fqNameUnsafe.asString() == fqMethodName
}

fun isAutoCreatedItUsage(expression: KtNameReferenceExpression): Boolean {
    if (expression.getReferencedName() != "it") return false
    val context = expression.analyze(BodyResolveMode.PARTIAL)
    val target = expression.mainReference.resolveToDescriptors(context).singleOrNull() as? ValueParameterDescriptor? ?: return false
    return context[BindingContext.AUTO_CREATED_IT, target]!!
}

// returns assignment which replaces initializer
fun splitPropertyDeclaration(property: KtProperty): KtBinaryExpression {
    val parent = property.parent

    val initializer = property.initializer!!

    val explicitTypeToSet = if (property.typeReference != null) null else initializer.analyze().getType(initializer)

    val psiFactory = KtPsiFactory(property)
    var assignment = psiFactory.createExpressionByPattern("$0 = $1", property.nameAsName!!, initializer)

    assignment = parent.addAfter(assignment, property) as KtBinaryExpression
    parent.addAfter(psiFactory.createNewLine(), property)

    property.initializer = null

    if (explicitTypeToSet != null) {
        property.setType(explicitTypeToSet)
    }

    return assignment
}

val KtQualifiedExpression.callExpression: KtCallExpression?
    get() = selectorExpression as? KtCallExpression

val KtQualifiedExpression.calleeName: String?
    get() = (callExpression?.calleeExpression as? KtNameReferenceExpression)?.text

fun KtQualifiedExpression.toResolvedCall(bodyResolveMode: BodyResolveMode): ResolvedCall<out CallableDescriptor>? {
    val callExpression = callExpression ?: return null
    return callExpression.getResolvedCall(callExpression.analyze(bodyResolveMode)) ?: return null
}

fun KtExpression.isExitStatement(): Boolean {
    when (this) {
        is KtContinueExpression, is KtBreakExpression, is KtThrowExpression, is KtReturnExpression -> return true
        else -> return false
    }
}

// returns false for call of super, static method or method from package
fun KtQualifiedExpression.isReceiverExpressionWithValue(): Boolean {
    val receiver = receiverExpression
    if (receiver is KtSuperExpression) return false
    return analyze().getType(receiver) != null
}

fun KtExpression.negate(): KtExpression {
    val specialNegation = specialNegation()
    if (specialNegation != null) return specialNegation
    return KtPsiFactory(this).createExpressionByPattern("!$0", this)
}

fun KtExpression.resultingWhens(): List<KtWhenExpression> = when (this) {
    is KtWhenExpression -> listOf(this) + entries.map { it.expression?.resultingWhens() ?: listOf() }.flatten()
    is KtIfExpression -> (then?.resultingWhens() ?: listOf()) + (`else`?.resultingWhens() ?: listOf())
    is KtBinaryExpression -> (left?.resultingWhens() ?: listOf()) + (right?.resultingWhens() ?: listOf())
    is KtUnaryExpression -> this.baseExpression?.resultingWhens() ?: listOf()
    is KtBlockExpression -> statements.lastOrNull()?.resultingWhens() ?: listOf()
    else -> listOf()
}

fun KtExpression?.hasResultingIfWithoutElse(): Boolean = when (this) {
    is KtIfExpression -> `else` == null || then.hasResultingIfWithoutElse() || `else`.hasResultingIfWithoutElse()
    is KtWhenExpression -> entries.any { it.expression.hasResultingIfWithoutElse() }
    is KtBinaryExpression -> left.hasResultingIfWithoutElse() || right.hasResultingIfWithoutElse()
    is KtUnaryExpression -> baseExpression.hasResultingIfWithoutElse()
    is KtBlockExpression -> statements.lastOrNull().hasResultingIfWithoutElse()
    else -> false
}

private fun KtExpression.specialNegation(): KtExpression? {
    val factory = KtPsiFactory(this)
    when (this) {
        is KtPrefixExpression -> {
            if (operationReference.getReferencedName() == "!") {
                val baseExpression = baseExpression
                if (baseExpression != null) {
                    val bindingContext = baseExpression.analyze(BodyResolveMode.PARTIAL)
                    val type = bindingContext.getType(baseExpression)
                    if (type != null && KotlinBuiltIns.isBoolean(type)) {
                        return KtPsiUtil.safeDeparenthesize(baseExpression)
                    }
                }
            }
        }

        is KtBinaryExpression -> {
            val operator = operationToken
            if (operator !in NEGATABLE_OPERATORS) return null
            val left = left ?: return null
            val right = right ?: return null
            return factory.createExpressionByPattern("$0 $1 $2", left, getNegatedOperatorText(operator), right)
        }

        is KtIsExpression -> {
            return factory.createExpressionByPattern("$0 $1 $2",
                                                     leftHandSide,
                                                     if (isNegated) "is" else "!is",
                                                     typeReference ?: return null)
        }

        is KtConstantExpression -> {
            return when (text) {
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
        KtTokens.EQEQ -> KtTokens.EXCLEQ.value
        KtTokens.EXCLEQ -> KtTokens.EQEQ.value
        KtTokens.EQEQEQ -> KtTokens.EXCLEQEQEQ.value
        KtTokens.EXCLEQEQEQ -> KtTokens.EQEQEQ.value
        KtTokens.IS_KEYWORD -> KtTokens.NOT_IS.value
        KtTokens.NOT_IS -> KtTokens.IS_KEYWORD.value
        KtTokens.IN_KEYWORD -> KtTokens.NOT_IN.value
        KtTokens.NOT_IN -> KtTokens.IN_KEYWORD.value
        KtTokens.LT -> KtTokens.GTEQ.value
        KtTokens.LTEQ -> KtTokens.GT.value
        KtTokens.GT -> KtTokens.LTEQ.value
        KtTokens.GTEQ -> KtTokens.LT.value
        else -> throw IllegalArgumentException("The token $token does not have a negated equivalent.")
    }
}

internal fun KotlinType.isFlexibleRecursive(): Boolean {
    if (isFlexible()) return true
    return arguments.any { !it.isStarProjection && it.type.isFlexibleRecursive() }
}

val KtIfExpression.branches: List<KtExpression?> get() = ifBranchesOrThis()

private fun KtExpression.ifBranchesOrThis(): List<KtExpression?> {
    if (this !is KtIfExpression) return listOf(this)
    return listOf(then) + `else`?.ifBranchesOrThis().orEmpty()
}

fun ResolvedCall<out CallableDescriptor>.resolvedToArrayType(): Boolean =
        resultingDescriptor.returnType.let { type ->
            type != null && (KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type))
        }

fun KtElement?.isZero() = this?.text == "0"

fun KtElement?.isOne() = this?.text == "1"

private fun KtExpression.isExpressionOfTypeOrSubtype(predicate: (KotlinType) -> Boolean): Boolean {
    val returnType = getResolvedCall(analyze())?.resultingDescriptor?.returnType
    return returnType != null && (returnType.constructor.supertypes + returnType).any(predicate)
}

fun KtElement?.isSizeOrLength(): Boolean {
    if (this !is KtDotQualifiedExpression) return false

    return when (selectorExpression?.text) {
        "size" -> receiverExpression.isExpressionOfTypeOrSubtype { type ->
            KotlinBuiltIns.isArray(type) ||
            KotlinBuiltIns.isPrimitiveArray(type) ||
            KotlinBuiltIns.isCollectionOrNullableCollection(type) ||
            KotlinBuiltIns.isMapOrNullableMap(type)
        }
        "length" -> receiverExpression.isExpressionOfTypeOrSubtype(KotlinBuiltIns::isCharSequenceOrNullableCharSequence)
        else -> false
    }
}


fun KtDotQualifiedExpression.getLeftMostReceiverExpression(): KtExpression =
        (receiverExpression as? KtDotQualifiedExpression)?.getLeftMostReceiverExpression() ?: receiverExpression

fun KtDotQualifiedExpression.replaceFirstReceiver(
        factory: KtPsiFactory,
        newReceiver: KtExpression,
        safeAccess: Boolean = false
): KtExpression {
    val replaced = (if (safeAccess) {
        this.replaced(factory.createExpressionByPattern("$0?.$1", receiverExpression, selectorExpression!!))
    } else this) as KtQualifiedExpression
    val receiver = replaced.receiverExpression
    when (receiver) {
        is KtDotQualifiedExpression -> {
            receiver.replace(receiver.replaceFirstReceiver(factory, newReceiver, safeAccess))
        }
        else -> {
            receiver.replace(newReceiver)
        }
    }
    return replaced
}

fun KtDotQualifiedExpression.deleteFirstReceiver(): KtExpression {
    val receiver = receiverExpression
    when (receiver) {
        is KtDotQualifiedExpression -> receiver.deleteFirstReceiver()
        else -> selectorExpression?.let { return this.replace(it) as KtExpression }
    }
    return this
}
