/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

object FirErrors {
    val UNRESOLVED_REFERENCE by error1<FirSourceElement, PsiElement, String?>()
    val INAPPLICABLE_CANDIDATE by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val AMBIGUITY by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val ASSIGN_OPERATOR_AMBIGUITY by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val SYNTAX_ERROR by error0<FirSourceElement, PsiElement>()
    val UNRESOLVED_LABEL by error0<FirSourceElement, PsiElement>()
    val ILLEGAL_CONST_EXPRESSION by error0<FirSourceElement, PsiElement>()
    val DESERIALIZATION_ERROR by error0<FirSourceElement, PsiElement>()
    val INFERENCE_ERROR by error0<FirSourceElement, PsiElement>()
    val TYPE_PARAMETER_AS_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val ENUM_AS_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val RECURSION_IN_SUPERTYPES by error0<FirSourceElement, PsiElement>()
    val RECURSION_IN_IMPLICIT_TYPES by error0<FirSourceElement, PsiElement>()
    val ERROR_FROM_JAVA_RESOLUTION by error0<FirSourceElement, PsiElement>()
    val EXPRESSION_REQUIRED by error0<FirSourceElement, PsiElement>()
    val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by error0<FirSourceElement, PsiElement>()
    val NOT_A_LOOP_LABEL by error0<FirSourceElement, PsiElement>()
    val OTHER_ERROR by error0<FirSourceElement, PsiElement>()
    val TYPE_MISMATCH by error2<FirSourceElement, PsiElement, ConeKotlinType, ConeKotlinType>()
    val VARIABLE_EXPECTED by error0<FirSourceElement, PsiElement>()
    val RETURN_NOT_ALLOWED by error0<FirSourceElement, PsiElement>()
    val SUPER_IS_NOT_AN_EXPRESSION by error0<FirSourceElement, PsiElement>()
    val SUPER_NOT_AVAILABLE by error0<FirSourceElement, PsiElement>()
    val NOT_A_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE by error0<FirSourceElement, PsiElement>()

    val INAPPLICABLE_INFIX_MODIFIER by existing<FirSourceElement, PsiElement, String>(Errors.INAPPLICABLE_INFIX_MODIFIER)
    val CONSTRUCTOR_IN_OBJECT by existing<FirSourceElement, KtDeclaration>(Errors.CONSTRUCTOR_IN_OBJECT)
    val CONSTRUCTOR_IN_INTERFACE by existing<FirSourceElement, KtDeclaration>(Errors.CONSTRUCTOR_IN_INTERFACE)
    val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by existing<FirSourceElement, PsiElement>(Errors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM)
    val NON_PRIVATE_CONSTRUCTOR_IN_SEALED by existing<FirSourceElement, PsiElement>(Errors.NON_PRIVATE_CONSTRUCTOR_IN_SEALED)

    val REPEATED_MODIFIER by error1<FirSourceElement, PsiElement, KtModifierKeywordToken>()
    val REDUNDANT_MODIFIER by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER_PAIR by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val INCOMPATIBLE_MODIFIERS by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
}
