package org.jetbrains.jet.plugin.completion.smart

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.types.*
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.completion.*
import java.util.*
import org.jetbrains.jet.plugin.completion.DescriptorLookupConverter
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

enum class Tail {
    COMMA
    PARENTHESIS
}

data class ExpectedInfo(val `type`: JetType, val tail: Tail?)

class SmartCompletion(val expression: JetSimpleNameExpression,
                      val resolveSession: ResolveSessionForBodies,
                      val visibilityFilter: (DeclarationDescriptor) -> Boolean) {

    private val bindingContext = resolveSession.resolveToElement(expression)
    private val moduleDescriptor = resolveSession.getModuleDescriptor()
    private val project = expression.getProject()

    public fun buildLookupElements(referenceVariants: Iterable<DeclarationDescriptor>): Collection<LookupElement>? {
        val parent = expression.getParent()
        val expressionWithType: JetExpression
        val receiver: JetExpression?
        if (parent is JetQualifiedExpression) {
            expressionWithType = parent
            receiver = parent.getReceiverExpression()
        }
        else {
            expressionWithType = expression
            receiver = null
        }

        val allExpectedInfos = ExpectedInfos(bindingContext, moduleDescriptor).calculate(expressionWithType) ?: return null
        val expectedInfos = allExpectedInfos.filter { !it.`type`.isError() }
        if (expectedInfos.isEmpty()) return null

        val result = ArrayList<LookupElement>()

        val typesWithAutoCasts: (DeclarationDescriptor) -> Iterable<JetType> = TypesWithAutoCasts(bindingContext).calculate(expressionWithType, receiver)

        val itemsToSkip = calcItemsToSkip(expressionWithType)

        val functionExpectedInfos = expectedInfos.filter { KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(it.`type`) }

        for (descriptor in referenceVariants) {
            if (itemsToSkip.contains(descriptor)) continue

            val matchedExpectedInfos = expectedInfos.filter { expectedInfo ->
                typesWithAutoCasts(descriptor).any { it.isSubtypeOf(expectedInfo.`type`) }
            }
            if (matchedExpectedInfos.isNotEmpty()) {
                val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor)
                result.add(addTailToLookupElement(lookupElement, matchedExpectedInfos))
            }

            if (receiver == null) {
                toFunctionReferenceLookupElement(descriptor, functionExpectedInfos)?.let { result.add(it) }
            }
        }

        if (receiver == null) {
            TypeInstantiationItems(bindingContext, resolveSession).addToCollection(result, expectedInfos)

            StaticMembers(bindingContext, resolveSession).addToCollection(result, expectedInfos, expression)

            ThisItems(bindingContext).addToCollection(result, expressionWithType, expectedInfos)

            LambdaItems(project).addToCollection(result, functionExpectedInfos)
        }

        return result
    }

    private fun calcItemsToSkip(expression: JetExpression): Collection<DeclarationDescriptor> {
        val parent = expression.getParent()
        when(parent) {
            is JetProperty -> {
                //TODO: this can be filtered out by ordinary completion
                if (expression == parent.getInitializer()) {
                    return resolveSession.resolveToElement(parent)[BindingContext.DECLARATION_TO_DESCRIPTOR, parent].toList()
                }
            }

            is JetBinaryExpression -> {
                if (parent.getRight() == expression && parent.getOperationToken() == JetTokens.EQ) {
                    val left = parent.getLeft()
                    if (left is JetReferenceExpression) {
                        return resolveSession.resolveToElement(left)[BindingContext.REFERENCE_TARGET, left].toList()
                    }
                }
            }
        }
        return listOf()
    }

    private fun toFunctionReferenceLookupElement(descriptor: DeclarationDescriptor,
                                                 functionExpectedInfos: Collection<ExpectedInfo>): LookupElement? {
        if (functionExpectedInfos.isEmpty()) return null

        fun toLookupElement(descriptor: FunctionDescriptor): LookupElement? {
            val functionType = functionType(descriptor)
            if (functionType == null) return null

            val matchedExpectedInfos = functionExpectedInfos.filter { functionType.isSubtypeOf(it.`type`) }
            if (matchedExpectedInfos.isEmpty()) return null

            var lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor)
            val text = "::" + (if (descriptor is ConstructorDescriptor) descriptor.getContainingDeclaration().getName() else descriptor.getName())
            lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = text

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.setItemText(text)
                    presentation.setTypeText("")
                }

                override fun handleInsert(context: InsertionContext) {
                }
            }

            return addTailToLookupElement(lookupElement, matchedExpectedInfos)
        }

        if (descriptor is SimpleFunctionDescriptor) {
            return toLookupElement(descriptor)
        }
        else if (descriptor is ClassDescriptor && descriptor.getModality() != Modality.ABSTRACT) {
            val constructors = descriptor.getConstructors().filter(visibilityFilter)
            if (constructors.size == 1) {
                //TODO: this code is to be changed if overloads to start work after ::
                return toLookupElement(constructors.single())
            }
        }

        return null
    }
}
