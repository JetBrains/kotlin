/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isVariadic
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class VariadicDeclarationsChecker : DeclarationChecker {
    private val unsupportedFunctionModifiers = setOf(
        KtTokens.OPERATOR_KEYWORD,
        KtTokens.INLINE_KEYWORD,
        KtTokens.ABSTRACT_KEYWORD,
        KtTokens.OVERRIDE_KEYWORD,
        KtTokens.OPEN_KEYWORD,
        KtTokens.INFIX_KEYWORD
    )

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration !is KtTypeParameterListOwner) return

        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.VariadicGenerics)) {
            reportUnsupportedOnVararg(declaration, context)
            return
        }

        when (descriptor) {
            is SimpleFunctionDescriptor -> {
                checkFunctionTypeParameters(declaration, descriptor, context)
                checkFunctionParameters(descriptor, context)
            }
            else -> checkTypeParametersOnNonVariadicDeclaration(declaration, context)
        }
    }

    private fun checkFunctionTypeParameters(
        declaration: KtTypeParameterListOwner,
        functionDescriptor: SimpleFunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        val variadicParameters = functionDescriptor.typeParameters.filter { it.isVariadic }
        if (variadicParameters.isEmpty())
            return

        if (variadicParameters.size > 1) {
            for (ktTypeParameter in declaration.typeParameters) {
                context.trace.report(Errors.MULTIPLE_VARARG_PARAMETERS.on(ktTypeParameter))
            }
        }

        checkUnsupprotedModifiers(declaration, context)
    }

    private fun checkUnsupprotedModifiers(
        declaration: KtTypeParameterListOwner,
        context: DeclarationCheckerContext
    ) {
        unsupportedFunctionModifiers.forEach { modifier ->
            declaration.modifierList?.getModifier(modifier)?.let { reportOn ->
                context.trace.report(
                    Errors.WRONG_MODIFIER_TARGET.on(
                        reportOn,
                        modifier,
                        "functions with variadic type parameters"
                    )
                )
            }
        }
    }

    private fun checkFunctionParameters(
        functionDescriptor: SimpleFunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        for (parameter in functionDescriptor.valueParameters) {
            val psiParameter = parameter.source.getPsi().safeAs<KtParameter>() ?: continue
            val typeReference = psiParameter.getChildOfType<KtTypeReference>() ?: continue
            val outerTupleType = typeReference.getChildOfType<KtTupleType>()

            val isVararg = parameter.isVararg
            val tupleTypes = psiParameter.collectDescendantsOfType<KtTupleType>()
            val dependsOnVariadicType = parameter.type.contains { it.isVariadic }

            if (tupleTypes.count() > 1) {
                context.trace.report(
                    Errors.UNSUPPORTED.on(typeReference, "Multiple spread (*) operators in single type are not supported")
                )
                continue
            }
            if (outerTupleType != null
                && (!isVararg || !dependsOnVariadicType)
            ) {
                context.trace.report(Errors.NON_VARIADIC_SPREAD.on(outerTupleType))
                continue
            }
            if (dependsOnVariadicType && tupleTypes.count() == 0) {
                context.trace.report(Errors.NO_SPREAD_FOR_VARIADIC_PARAMETER.on(typeReference))
            }
        }
    }

    private fun reportUnsupportedOnVararg(declaration: KtTypeParameterListOwner, context: DeclarationCheckerContext) {
        for (ktTypeParameter in declaration.typeParameters) {
            val ktVarargKeyword = ktTypeParameter.modifierList?.getModifier(KtTokens.VARARG_KEYWORD)
                ?: continue
            context.trace.report(
                Errors.UNSUPPORTED.on(ktVarargKeyword, "'vararg' modifier on type parameter is not supported")
            )
        }
    }

    private fun checkTypeParametersOnNonVariadicDeclaration(
        declaration: KtTypeParameterListOwner,
        context: DeclarationCheckerContext
    ) {
        val ktTypeParameters = declaration.typeParameters
        for (typeParameter in ktTypeParameters) {
            if (typeParameter.hasModifier(KtTokens.VARARG_KEYWORD)) {
                val reportOn = typeParameter.modifierList?.getModifier(KtTokens.VARARG_KEYWORD) ?: typeParameter
                context.trace.report(
                    Errors.WRONG_MODIFIER_TARGET.on(
                        reportOn,
                        KtTokens.VARARG_KEYWORD,
                        "type parameters of non-function declarations"
                    )
                )
            }
        }
    }
}
