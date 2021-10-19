/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtExpressionTypeProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.resolve.calls.util.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForAbstractMethod
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class KtFe10ExpressionTypeProvider(override val analysisSession: KtFe10AnalysisSession) : KtExpressionTypeProvider() {
    private companion object {
        val NON_EXPRESSION_CONTAINERS = arrayOf(
            KtImportDirective::class.java,
            KtTypeReference::class.java,
            KtPackageDirective::class.java,
            KtLabelReferenceExpression::class.java
        )
    }

    override val token: ValidityToken
        get() = analysisSession.token

    private val builtIns: KotlinBuiltIns
        get() = analysisSession.resolveSession.moduleDescriptor.builtIns

    override fun getKtExpressionType(expression: KtExpression): KtType? = withValidityAssertion {
        // Not sure if it's safe enough. In theory, some annotations on expressions might change its type
        val unwrapped = expression.unwrapParenthesesLabelsAndAnnotations() as? KtExpression ?: return null
        if (unwrapped.getParentOfTypes(false, *NON_EXPRESSION_CONTAINERS) != null) {
            return null
        }

        val bindingContext = analysisSession.analyze(unwrapped, AnalysisMode.PARTIAL)
        val kotlinType = expression.getType(bindingContext) ?: builtIns.unitType
        return kotlinType.toKtType(analysisSession)
    }

    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType = withValidityAssertion {
        // Handle callable declarations with explicit return type first
        if (declaration is KtCallableDeclaration) {
            val typeReference = declaration.typeReference

            if (typeReference != null) {
                val bindingContext = analysisSession.analyze(typeReference, AnalysisMode.PARTIAL)
                val kotlinType = bindingContext[BindingContext.TYPE, typeReference]
                    ?: ErrorUtils.createErrorType("Return type \"${typeReference.text}\" cannot be resolved")

                return kotlinType.toKtType(analysisSession)
            }
        }

        if (declaration is KtFunction && declaration !is KtConstructor<*> && declaration.equalsToken != null) {
            val bindingContext = analysisSession.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.FUNCTION, declaration]?.returnType
                ?: ErrorUtils.createErrorType("Implicit return type for function \"${declaration.name}\" cannot be resolved")

            return kotlinType.toKtType(analysisSession)
        }

        if (declaration is KtProperty) {
            val bindingContext = analysisSession.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.VARIABLE, declaration]?.returnType
                ?: ErrorUtils.createErrorType("Implicit return type for property \"${declaration.name}\" cannot be resolved")

            return kotlinType.toKtType(analysisSession)
        }

        if (declaration is KtPropertyAccessor) {
            val bindingContext = analysisSession.analyze(declaration)
            val kotlinType = bindingContext[BindingContext.PROPERTY_ACCESSOR, declaration]?.returnType
                ?: ErrorUtils.createErrorType("Return type for property accessor \"${declaration.property.name}\" cannot be resolved")

            return kotlinType.toKtType(analysisSession)
        }

        return builtIns.unitType.toKtType(analysisSession)
    }

    override fun getFunctionalTypeForKtFunction(declaration: KtFunction): KtType = withValidityAssertion {
        val analysisMode = if (declaration.hasDeclaredReturnType()) AnalysisMode.PARTIAL else AnalysisMode.FULL
        val bindingContext = analysisSession.analyze(declaration, analysisMode)
        val functionDescriptor = bindingContext[BindingContext.FUNCTION, declaration]

        if (functionDescriptor != null) {
            return getFunctionTypeForAbstractMethod(functionDescriptor, false).toKtType(analysisSession)
        }

        val parameterCount = declaration.valueParameters.size + (if (declaration.isExtensionDeclaration()) 1 else 0)

        val function = when {
            declaration.hasModifier(KtTokens.SUSPEND_KEYWORD) -> builtIns.getSuspendFunction(parameterCount)
            else -> builtIns.getFunction(parameterCount)
        }

        val errorMessage = "Descriptor not found for function \"${declaration.name}\""
        return ErrorUtils.createErrorTypeWithCustomConstructor(errorMessage, function.typeConstructor).toKtType(analysisSession)
    }

    override fun getExpectedType(expression: PsiElement): KtType? = withValidityAssertion {
        val ktExpression = expression.getParentOfType<KtExpression>(false) ?: return null
        val parentExpression = ktExpression.parent

        // Unwrap specific expressions
        when (ktExpression) {
            is KtNameReferenceExpression -> {
                if (parentExpression is KtDotQualifiedExpression && parentExpression.selectorExpression == ktExpression) {
                    return getExpectedType(parentExpression)
                }
            }
            is KtFunctionLiteral -> {
                return getExpectedType(ktExpression.parent)
            }
        }

        if (parentExpression is KtCallableDeclaration) {
            if (expression is KtBlockExpression) {
                return null
            }

            val bindingContext = analysisSession.analyze(parentExpression)
            val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parentExpression]
            if (descriptor is CallableDescriptor) {
                return descriptor.returnType?.takeIf { !it.isError }?.toKtType(analysisSession)
            }
        } else if (parentExpression is KtBinaryExpressionWithTypeRHS && KtPsiUtil.isCast(parentExpression)) {
            val typeReference = parentExpression.right
            if (typeReference != null) {
                val bindingContext = analysisSession.analyze(typeReference)
                var kotlinType = bindingContext[BindingContext.TYPE, typeReference]
                if (kotlinType != null && KtPsiUtil.isSafeCast(parentExpression)) {
                    kotlinType = kotlinType.makeNullable()
                }
                return kotlinType?.toKtType(analysisSession)
            }
        } else if (parentExpression is KtValueArgument) {
            val callExpression = getContainingCallExpression(parentExpression)
            if (callExpression != null) {
                val bindingContext = analysisSession.analyze(callExpression)
                val resolvedCall = callExpression.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    val parameterDescriptor = resolvedCall.getParameterForArgument(parentExpression)?.original
                    if (parameterDescriptor != null) {
                        val kotlinType = when (val originalCallableDescriptor = parameterDescriptor.containingDeclaration) {
                            is SamConstructorDescriptor -> originalCallableDescriptor.returnTypeOrNothing
                            else -> parameterDescriptor.type
                        }
                        return kotlinType.takeIf { !it.isError }?.toKtType(analysisSession)
                    }
                }
            }
        }

        val bindingContext = analysisSession.analyze(ktExpression)
        val kotlinType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, ktExpression]
        return kotlinType?.takeIf { !it.isError }?.toKtType(analysisSession)
    }

    private fun getContainingCallExpression(argument: KtValueArgument): KtCallExpression? {
        return when (val parent = argument.parent) {
            is KtCallExpression -> parent
            is KtValueArgumentList -> parent.parent as? KtCallExpression
            else -> null
        }
    }

    override fun isDefinitelyNull(expression: KtExpression): Boolean = withValidityAssertion {
        val unwrapped = expression.unwrapParenthesesLabelsAndAnnotations() as? KtElement ?: return false
        val bindingContext = analysisSession.analyze(expression, AnalysisMode.PARTIAL)

        if (bindingContext[BindingContext.SMARTCAST_NULL, expression] == true) {
            return true
        }

        for (diagnostic in bindingContext.diagnostics.forElement(unwrapped)) {
            if (diagnostic.factory == Errors.ALWAYS_NULL) {
                return true
            }
        }

        return false
    }

    override fun isDefinitelyNotNull(expression: KtExpression): Boolean = withValidityAssertion {
        val ktExpression = expression as? KtExpression ?: return false
        val bindingContext = analysisSession.analyze(ktExpression)

        val smartCasts = bindingContext[BindingContext.SMARTCAST, ktExpression]

        if (smartCasts is MultipleSmartCasts) {
            if (smartCasts.map.values.all { !it.isMarkedNullable }) {
                return true
            }
        }

        val smartCastType = smartCasts?.defaultType
        if (smartCastType != null && !smartCastType.isMarkedNullable) {
            return true
        }

        val expressionType = expression.getType(bindingContext) ?: return false
        return !TypeUtils.isNullableType(expressionType)
    }
}