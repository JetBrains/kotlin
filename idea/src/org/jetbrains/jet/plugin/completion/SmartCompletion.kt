package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.util.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.CancelableResolveSession
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.checker.JetTypeChecker

trait SmartCompletionFilter{
    fun accepts(descriptor: DeclarationDescriptor): Boolean
}

fun buildSmartCompletionFilter(expression: JetSimpleNameExpression, resolveSession: CancelableResolveSession): SmartCompletionFilter? {
    val parent = expression.getParent()
    val expressionWithType = if (parent is JetQualifiedExpression) parent else expression
    val expectedType: JetType? = resolveSession.resolveToElement(expressionWithType).get(BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType)
    if (expectedType == null) return null

    val itemsToSkip = calcItemsToSkip(expressionWithType, resolveSession)

    return object: SmartCompletionFilter{
        override fun accepts(descriptor: DeclarationDescriptor): Boolean {
            if (itemsToSkip.contains(descriptor)) return false

            if (descriptor is CallableDescriptor) {
                val returnType = descriptor.getReturnType()
                return returnType != null && JetTypeChecker.INSTANCE.isSubtypeOf(returnType, expectedType)
            }
            else {
                return false
            }
        }
    }
}

private fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()

private fun calcItemsToSkip(expression: JetExpression, resolveSession: CancelableResolveSession): Collection<DeclarationDescriptor> {
    val parent = expression.getParent()
    when(parent) {
        is JetProperty -> {
            //TODO: this can be filtered out by ordinary completion
            if (expression == parent.getInitializer()) {
                return resolveSession.resolveToElement(parent).get(BindingContext.DECLARATION_TO_DESCRIPTOR, parent).toList()
            }
        }

        is JetBinaryExpression -> {
            if (parent.getRight() == expression && parent.getOperationToken() == JetTokens.EQ) {
                val left = parent.getLeft()
                if (left is JetReferenceExpression) {
                    return resolveSession.resolveToElement(left).get(BindingContext.REFERENCE_TARGET, left).toList()
                }
            }
        }
    }
    return listOf()
}