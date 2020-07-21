/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirEffectiveVisibility
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*

object FirErrors {
    val UNRESOLVED_REFERENCE by error1<FirSourceElement, PsiElement, String>()
    val INAPPLICABLE_CANDIDATE by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val AMBIGUITY by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val ASSIGN_OPERATOR_AMBIGUITY by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val SYNTAX_ERROR by error0<FirSourceElement, PsiElement>()
    val UNRESOLVED_LABEL by error0<FirSourceElement, PsiElement>()
    val ILLEGAL_CONST_EXPRESSION by error0<FirSourceElement, PsiElement>()
    val ILLEGAL_UNDERSCORE by error0<FirSourceElement, PsiElement>()
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
    val ABSTRACT_SUPER_CALL by error0<FirSourceElement, PsiElement>()
    val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE by error0<FirSourceElement, PsiElement>()

    val INAPPLICABLE_INFIX_MODIFIER by existing<FirSourceElement, PsiElement, String>(Errors.INAPPLICABLE_INFIX_MODIFIER)
    val CONSTRUCTOR_IN_OBJECT by existing<FirSourceElement, KtDeclaration>(Errors.CONSTRUCTOR_IN_OBJECT)
    val CONSTRUCTOR_IN_INTERFACE by existing<FirSourceElement, KtDeclaration>(Errors.CONSTRUCTOR_IN_INTERFACE)
    val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by existing<FirSourceElement, PsiElement>(Errors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM)
    val NON_PRIVATE_CONSTRUCTOR_IN_SEALED by existing<FirSourceElement, PsiElement>(Errors.NON_PRIVATE_CONSTRUCTOR_IN_SEALED)

    val ANNOTATION_CLASS_MEMBER by existing<FirSourceElement, PsiElement>(Errors.ANNOTATION_CLASS_MEMBER)
    val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT by existing<FirSourceElement, KtExpression>(Errors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT)
    val LOCAL_ANNOTATION_CLASS_ERROR by existing<FirSourceElement, KtClassOrObject>(Errors.LOCAL_ANNOTATION_CLASS_ERROR)
    val MISSING_VAL_ON_ANNOTATION_PARAMETER by existing<FirSourceElement, KtParameter>(Errors.MISSING_VAL_ON_ANNOTATION_PARAMETER)
    val NULLABLE_TYPE_OF_ANNOTATION_MEMBER by existing<FirSourceElement, KtTypeReference>(Errors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER)
    val INVALID_TYPE_OF_ANNOTATION_MEMBER by existing<FirSourceElement, KtTypeReference>(Errors.INVALID_TYPE_OF_ANNOTATION_MEMBER)
    val VAR_ANNOTATION_PARAMETER by existing<FirSourceElement, KtParameter>(Errors.VAR_ANNOTATION_PARAMETER)

    // Exposed visibility group
    val EXPOSED_TYPEALIAS_EXPANDED_TYPE by error3<FirSourceElement, PsiElement, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_FUNCTION_RETURN_TYPE by error3<FirSourceElement, PsiElement, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_RECEIVER_TYPE by error3<FirSourceElement, KtTypeReference, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_PROPERTY_TYPE by error3<FirSourceElement, PsiElement, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_PARAMETER_TYPE by error3<FirSourceElement, KtParameter, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_SUPER_INTERFACE by error3<FirSourceElement, KtSuperTypeListEntry, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_SUPER_CLASS by error3<FirSourceElement, KtSuperTypeListEntry, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_TYPE_PARAMETER_BOUND by error3<FirSourceElement, KtTypeParameter, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()

    val REPEATED_MODIFIER by error1<FirSourceElement, PsiElement, KtModifierKeywordToken>()
    val REDUNDANT_MODIFIER by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER_PAIR by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val INCOMPATIBLE_MODIFIERS by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()

    val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by error0<FirSourceElement, PsiElement>()
    val UPPER_BOUND_VIOLATED by error0<FirSourceElement, PsiElement>()

    // Control flow diagnostics
    val UNINITIALIZED_VARIABLE by error1<FirSourceElement, PsiElement, FirPropertySymbol>()

    // Extended checkers group
    val REDUNDANT_VISIBILITY_MODIFIER by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_MODALITY_MODIFIER by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_RETURN_UNIT_TYPE by warning0<FirSourceElement, PsiTypeElement>()
    val REDUNDANT_EXPLICIT_TYPE by warning0<FirSourceElement, PsiElement>()
}


