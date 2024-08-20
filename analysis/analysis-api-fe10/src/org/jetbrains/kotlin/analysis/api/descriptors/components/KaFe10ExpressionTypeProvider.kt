/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.resolve.calls.smartcasts.SingleSmartCast
import org.jetbrains.kotlin.resolve.calls.util.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForAbstractMethod
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.typeUtil.makeNullable

internal class KaFe10ExpressionTypeProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaSessionComponent<KaFe10Session>(), KaExpressionTypeProvider, KaFe10SessionComponent {
    private companion object {
        val NON_EXPRESSION_CONTAINERS = arrayOf(
            KtImportDirective::class.java,
            KtTypeReference::class.java,
            KtPackageDirective::class.java,
            KtLabelReferenceExpression::class.java
        )
    }

    override val KtExpression.expressionType: KaType?
        get() = withValidityAssertion {
            // Not sure if it's safe enough. In theory, some annotations on expressions might change its type
            val unwrapped = unwrapParenthesesLabelsAndAnnotations() as? KtExpression ?: return null
            if (unwrapped.getParentOfTypes(false, *NON_EXPRESSION_CONTAINERS) != null) {
                return null
            }

            val bindingContext = analysisContext.analyze(unwrapped, AnalysisMode.PARTIAL)
            val smartCastType = when (val smartCastType = bindingContext[BindingContext.SMARTCAST, this]) {
                is SingleSmartCast -> smartCastType.type
                is MultipleSmartCasts -> intersectWrappedTypes(smartCastType.map.values)
                else -> null
            }
            val kotlinType = smartCastType ?: getType(bindingContext) ?: analysisContext.builtIns.unitType
            return kotlinType.toKtType(analysisContext)
        }

    override val KtDeclaration.returnType: KaType
        get() = withValidityAssertion { getReturnTypeForKtDeclaration(this) }

    private fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KaType {
        // Handle callable declarations with explicit return type first
        if (declaration is KtCallableDeclaration) {
            val typeReference = declaration.typeReference

            if (typeReference != null) {
                val bindingContext = analysisContext.analyze(typeReference, AnalysisMode.PARTIAL)
                val kotlinType =
                    if (declaration is KtParameter && declaration.isVarArg) {
                        // we want full Array<out T> type for parity with FIR implementation
                        bindingContext[BindingContext.VALUE_PARAMETER, declaration]?.returnType
                    } else {
                        bindingContext[BindingContext.TYPE, typeReference]
                    } ?: ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE, typeReference.text)

                return kotlinType.toKtType(analysisContext)
            }
        }

        if (declaration is KtFunction && declaration !is KtConstructor<*> && declaration.equalsToken != null) {
            val bindingContext = analysisContext.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.FUNCTION, declaration]?.returnType
                ?: ErrorUtils.createErrorType(ErrorTypeKind.IMPLICIT_RETURN_TYPE_FOR_FUNCTION, declaration.name ?: "<unknown>")

            return kotlinType.toKtType(analysisContext)
        }

        if (declaration is KtProperty) {
            val bindingContext = analysisContext.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.VARIABLE, declaration]?.returnType
                ?: ErrorUtils.createErrorType(ErrorTypeKind.IMPLICIT_RETURN_TYPE_FOR_PROPERTY, declaration.name ?: "<unknown>")

            return kotlinType.toKtType(analysisContext)
        }

        if (declaration is KtPropertyAccessor) {
            val bindingContext = analysisContext.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.PROPERTY_ACCESSOR, declaration]?.returnType
                ?: ErrorUtils.createErrorType(
                    ErrorTypeKind.IMPLICIT_RETURN_TYPE_FOR_PROPERTY_ACCESSOR, declaration.property.name ?: "<unknown>"
                )

            return kotlinType.toKtType(analysisContext)
        }

        // Manually handle custom setter parameter
        if (declaration is KtParameter) {
            val parameterList = declaration.parent as? KtParameterList
            if (parameterList?.parameters?.singleOrNull() == declaration) {
                val propertyAccessor = parameterList.parent as? KtPropertyAccessor
                val property = propertyAccessor?.parent as? KtProperty
                if (property != null && property.setter == propertyAccessor) {
                    val bindingContext = analysisContext.analyze(property)
                    val kotlinType = bindingContext[BindingContext.VARIABLE, property]?.returnType
                        ?: ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_PROPERTY, declaration.name ?: "<unknown>")

                    return kotlinType.toKtType(analysisContext)
                }
            }
        }

        if (declaration is KtConstructor<*>) {
            val bindingContext = analysisContext.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.CONSTRUCTOR, declaration]?.returnType
                ?: ErrorUtils.createErrorType(
                    ErrorTypeKind.RETURN_TYPE_FOR_CONSTRUCTOR, declaration.containingClass()?.name ?: "<unknown>"
                )
            return kotlinType.toKtType(analysisContext)
        }

        return analysisContext.builtIns.unitType.toKtType(analysisContext)
    }

    override val KtFunction.functionType: KaType
        get() = withValidityAssertion {
            val analysisMode = if (hasDeclaredReturnType()) AnalysisMode.PARTIAL else AnalysisMode.FULL
            val bindingContext = analysisContext.analyze(this, analysisMode)
            val functionDescriptor = bindingContext[BindingContext.FUNCTION, this]

            if (functionDescriptor != null) {
                return getFunctionTypeForAbstractMethod(functionDescriptor, false).toKtType(analysisContext)
            }

            val parameterCount = valueParameters.size + (if (isExtensionDeclaration()) 1 else 0)

            val function = when {
                hasModifier(KtTokens.SUSPEND_KEYWORD) -> analysisContext.builtIns.getSuspendFunction(parameterCount)
                else -> analysisContext.builtIns.getFunction(parameterCount)
            }

            val errorMessage = "Descriptor not found for function \"${name}\""
            return ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_FUNCTION, function.typeConstructor, errorMessage)
                .toKtType(analysisContext)
        }

    override val PsiElement.expectedType: KaType?
        get() = withValidityAssertion { computeExpectedType(this) }

    private fun computeExpectedType(expression: PsiElement): KaType? {
        val ktExpression = expression.getParentOfType<KtExpression>(false) ?: return null
        val parentExpression = if (ktExpression.parent is KtLabeledExpression) {
            // lambda -> labeled expression -> lambda argument (value argument)
            ktExpression.parent.parent
        } else {
            ktExpression.parent
        }

        // Unwrap specific expressions
        when (ktExpression) {
            is KtNameReferenceExpression -> {
                if (parentExpression is KtDotQualifiedExpression && parentExpression.selectorExpression == ktExpression) {
                    return computeExpectedType(parentExpression)
                }
            }
            is KtFunctionLiteral -> {
                return computeExpectedType(ktExpression.parent)
            }
        }

        when (parentExpression) {
            is KtCallableDeclaration -> {
                if (expression is KtBlockExpression) {
                    return null
                }

                if (parentExpression.typeReference == null) return null

                val bindingContext = analysisContext.analyze(parentExpression)
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parentExpression]
                if (descriptor is CallableDescriptor) {
                    return descriptor.returnType?.toKtNonErrorType(analysisContext)
                }
            }

            is KtBinaryExpressionWithTypeRHS -> {
                val typeReference = parentExpression.right
                if (KtPsiUtil.isCast(parentExpression) && typeReference != null) {
                    val bindingContext = analysisContext.analyze(typeReference)
                    var kotlinType = bindingContext[BindingContext.TYPE, typeReference]
                    if (kotlinType != null && KtPsiUtil.isSafeCast(parentExpression)) {
                        kotlinType = kotlinType.makeNullable()
                    }
                    return kotlinType?.toKtNonErrorType(analysisContext)
                }
            }

            is KtValueArgument -> {
                val callExpression = getContainingCallExpression(parentExpression)
                if (callExpression != null) {
                    val bindingContext = analysisContext.analyze(callExpression)
                    val resolvedCall = callExpression.getResolvedCall(bindingContext)
                    if (resolvedCall != null) {
                        val parameterDescriptor = resolvedCall.getParameterForArgument(parentExpression)?.original
                        if (parameterDescriptor != null) {
                            val kotlinType = when (val originalCallableDescriptor = parameterDescriptor.containingDeclaration) {
                                is SamConstructorDescriptor -> originalCallableDescriptor.returnTypeOrNothing
                                else -> {
                                    if (parameterDescriptor.isVararg)
                                        parameterDescriptor.varargElementType
                                    else
                                        parameterDescriptor.type
                                }
                            }
                            return kotlinType?.toKtNonErrorType(analysisContext)
                        }
                    }
                }
            }

            is KtWhenConditionWithExpression -> {
                val whenExpression = (parentExpression.parent as? KtWhenEntry)?.parent as? KtWhenExpression
                if (whenExpression != null) {
                    val subject = whenExpression.subjectExpression ?: return with(analysisSession) { builtinTypes.boolean }
                    val kotlinType = analysisContext.analyze(subject).getType(subject)
                    return kotlinType?.toKtNonErrorType(analysisContext)
                }
            }

            is KtBlockExpression -> {
                if (expression == parentExpression.statements.lastOrNull()) {
                    val functionLiteral = parentExpression.parent as? KtFunctionLiteral
                    if (functionLiteral != null) {
                        val functionType = computeExpectedType(functionLiteral) as? KaFunctionType
                        functionType?.returnType?.let { return it }
                    }
                }
            }

            is KtWhenEntry -> {
                if (expression == parentExpression.expression) {
                    val whenExpression = parentExpression.parent as? KtWhenExpression
                    if (whenExpression != null) {
                        computeExpectedType(whenExpression)?.let { return it }

                        val entries = whenExpression.entries
                        val entryExpressions = entries.mapNotNull { entry -> entry.expression?.takeUnless { expression == it } }
                        val kotlinTypes = entryExpressions.mapNotNull { analysisContext.analyze(it).getType(it) }
                        return intersectWrappedTypes(kotlinTypes).toKtNonErrorType(analysisContext)
                    }
                }
            }
        }

        val bindingContext = analysisContext.analyze(ktExpression)
        val kotlinType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, ktExpression]
        return kotlinType?.toKtNonErrorType(analysisContext)
    }

    private fun getContainingCallExpression(argument: KtValueArgument): KtCallExpression? {
        return when (val parent = argument.parent) {
            is KtCallExpression -> parent
            is KtValueArgumentList -> parent.parent as? KtCallExpression
            else -> null
        }
    }

    override val KtExpression.isDefinitelyNull: Boolean
        get() = withValidityAssertion {
            val unwrapped = unwrapParenthesesLabelsAndAnnotations() as? KtElement ?: return false
            val bindingContext = analysisContext.analyze(this, AnalysisMode.PARTIAL)

            if (bindingContext[BindingContext.SMARTCAST_NULL, this] == true) {
                return true
            }

            for (diagnostic in bindingContext.diagnostics.forElement(unwrapped)) {
                if (diagnostic.factory == Errors.ALWAYS_NULL) {
                    return true
                }
            }

            return false
        }

    override val KtExpression.isDefinitelyNotNull: Boolean
        get() = withValidityAssertion {
            val bindingContext = analysisContext.analyze(this)

            val smartCasts = bindingContext[BindingContext.SMARTCAST, this]

            if (smartCasts is MultipleSmartCasts) {
                if (smartCasts.map.values.all { !it.isMarkedNullable }) {
                    return true
                }
            }

            val smartCastType = smartCasts?.defaultType
            if (smartCastType != null && !smartCastType.isMarkedNullable) {
                return true
            }

            val expressionType = getType(bindingContext) ?: return false
            return !TypeUtils.isNullableType(expressionType)
        }

    private fun KotlinType.toKtNonErrorType(analysisContext: Fe10AnalysisContext): KaType? =
        this.toKtType(analysisContext).takeUnless { it is KaErrorType }
}