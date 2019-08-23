/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

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
        when (descriptor) {
            is SimpleFunctionDescriptor -> checkFunctionTypeParameters(declaration, descriptor, context)
            else -> checkClassifierTypeParameters(declaration, context)
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

        if (variadicParameters.size > 1) {
            for (ktTypeParameter in declaration.typeParameters) {
                context.trace.report(Errors.MULTIPLE_VARARG_PARAMETERS.on(ktTypeParameter))
            }
        }
    }

    private fun checkClassifierTypeParameters(
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
