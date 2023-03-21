/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirJsErrors {
    // Annotations
    val WRONG_JS_QUALIFIER by error0<KtElement>()
    val JS_MODULE_PROHIBITED_ON_VAR by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val JS_MODULE_PROHIBITED_ON_NON_NATIVE by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NESTED_JS_MODULE_PROHIBITED by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val RUNTIME_ANNOTATION_NOT_SUPPORTED by warning0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NATIVE_INDEXER_WRONG_PARAMETER_COUNT by error2<KtElement, Int, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val NATIVE_SETTER_WRONG_RETURN_TYPE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val JS_NAME_IS_NOT_ON_ALL_ACCESSORS by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val JS_NAME_PROHIBITED_FOR_NAMED_NATIVE by error0<KtElement>()
    val JS_NAME_PROHIBITED_FOR_OVERRIDE by error0<KtElement>()
    val JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED by error0<KtElement>()
    val JS_NAME_ON_ACCESSOR_AND_PROPERTY by error0<KtElement>()
    val JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY by error0<KtElement>()

    // Supertypes
    val WRONG_MULTIPLE_INHERITANCE by error1<KtElement, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // Fun Interfaces
    val IMPLEMENTING_FUNCTION_INTERFACE by error0<KtClassOrObject>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // External
    val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE by error1<KtElement, FirNamedFunctionSymbol>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION by error0<PsiElement>()
    val EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER by error0<KtParameter>()
    val EXTERNAL_ENUM_ENTRY_WITH_BODY by error0<KtElement>()
    val EXTERNAL_ANONYMOUS_INITIALIZER by error0<KtAnonymousInitializer>()
    val EXTERNAL_DELEGATION by error0<KtElement>()
    val EXTERNAL_DELEGATED_CONSTRUCTOR_CALL by error0<KtElement>()
    val WRONG_BODY_OF_EXTERNAL_DECLARATION by error0<KtElement>()
    val WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION by error0<KtElement>()
    val WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER by error0<KtElement>()
    val NESTED_EXTERNAL_DECLARATION by error0<KtExpression>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val WRONG_EXTERNAL_DECLARATION by error1<KtExpression, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NESTED_CLASS_IN_EXTERNAL_INTERFACE by error0<KtExpression>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val INLINE_EXTERNAL_DECLARATION by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING by warning0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING by warning0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val INLINE_CLASS_IN_EXTERNAL_DECLARATION by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE by error0<KtExpression>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val CANNOT_CHECK_FOR_EXTERNAL_INTERFACE by error1<KtElement, ConeKotlinType>()
    val UNCHECKED_CAST_TO_EXTERNAL_INTERFACE by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val EXTERNAL_INTERFACE_AS_CLASS_LITERAL by error0<KtElement>()
    val JS_EXTERNAL_INHERITORS_ONLY by error2<KtDeclaration, FirClassLikeSymbol<*>, FirClassLikeSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val JS_EXTERNAL_ARGUMENT by error1<KtExpression, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // Export
    val NESTED_JS_EXPORT by error0<KtElement>()
    val WRONG_EXPORTED_DECLARATION by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NON_EXPORTABLE_TYPE by warning2<KtElement, String, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NON_CONSUMABLE_EXPORTED_IDENTIFIER by warning1<KtElement, String>()

    // Dynamics
    val DELEGATION_BY_DYNAMIC by error0<KtElement>()
    val SPREAD_OPERATOR_IN_DYNAMIC_CALL by error0<KtElement>(SourceElementPositioningStrategies.SPREAD_OPERATOR)
    val WRONG_OPERATION_WITH_DYNAMIC by error1<KtElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirJsErrorsDefaultMessages)
    }
}
