package org.jetbrains.jet.plugin.completion

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.CancelableResolveSession
import org.jetbrains.jet.lang.types.*
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import com.intellij.codeInsight.lookup.*
import java.util.ArrayList
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.InsertHandler
import org.jetbrains.jet.plugin.completion.handlers.*
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement

trait SmartCompletionData{
    fun accepts(descriptor: DeclarationDescriptor): Boolean
    val additionalElements: Iterable<LookupElement>
}

fun buildSmartCompletionData(expression: JetSimpleNameExpression, resolveSession: CancelableResolveSession): SmartCompletionData? {
    val parent = expression.getParent()
    val expressionWithType = if (parent is JetQualifiedExpression) parent else expression
    val bindingContext = resolveSession.resolveToElement(expressionWithType)
    val expectedType: JetType? = bindingContext.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType)
    if (expectedType == null) return null

    val itemsToSkip = calcItemsToSkip(expressionWithType, resolveSession)

    val additionalElements = ArrayList<LookupElement>()

    if (expression == expressionWithType) { // no qualifier
        val typeConstructor: TypeConstructor = expectedType.getConstructor()
        val classifier: ClassifierDescriptor? = typeConstructor.getDeclarationDescriptor()
        if (classifier is ClassDescriptor) {
            if (classifier.getModality() != Modality.ABSTRACT){
                val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, classifier)

                val typeArgs = expectedType.getArguments()
                //TODO: shouldn't be method in DescriptorRenderer to render type arguments?
                val typeArgsText =
                        if (typeArgs.isEmpty())
                            ""
                        else
                            typeArgs.map { DescriptorRenderer.TEXT.renderType(it.getType()) }.makeString(", ", "<", ">")
                val presentableText = lookupElement.getLookupString() + typeArgsText + "()"

                val constructors: Collection<ConstructorDescriptor> = classifier.getConstructors()
                val caretPosition =
                        if (constructors.size == 0)
                            CaretPosition.AFTER_BRACKETS
                        else if (constructors.size == 1)
                            if (constructors.first().getValueParameters().isEmpty()) CaretPosition.AFTER_BRACKETS else CaretPosition.IN_BRACKETS
                        else
                            CaretPosition.IN_BRACKETS
                val insertHandler = JetFunctionInsertHandler(caretPosition, BracketType.PARENTHESIS)

                //TODO: very bad code
                if (lookupElement is LookupElementBuilder) {
                    additionalElements.add(lookupElement.withPresentableText(presentableText).withInsertHandler(insertHandler))
                }
                else if (lookupElement is JavaPsiClassReferenceElement) {
                    additionalElements.add(lookupElement.setPresentableText(presentableText).setInsertHandler(insertHandler))
                }
            }
        }
    }

    return object: SmartCompletionData{
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

        override val additionalElements: Iterable<LookupElement> = additionalElements
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