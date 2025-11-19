/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JS_DIAGNOSTICS_LIST]
 */
@Suppress("IncorrectFormatting")
object FirJsErrors : KtDiagnosticsContainer() {
    // Annotations
    val JS_MODULE_PROHIBITED_ON_NON_NATIVE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_MODULE_PROHIBITED_ON_NON_NATIVE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE: KtDiagnosticFactory0 = KtDiagnosticFactory0("CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val RUNTIME_ANNOTATION_NOT_SUPPORTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("RUNTIME_ANNOTATION_NOT_SUPPORTED", WARNING, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, PsiElement::class, getRendererFactory())
    val RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, PsiElement::class, getRendererFactory())
    val NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NATIVE_INDEXER_WRONG_PARAMETER_COUNT: KtDiagnosticFactory2<Int, String> = KtDiagnosticFactory2("NATIVE_INDEXER_WRONG_PARAMETER_COUNT", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE", ERROR, SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE, KtDeclaration::class, getRendererFactory())
    val NATIVE_SETTER_WRONG_RETURN_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NATIVE_SETTER_WRONG_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE, KtDeclaration::class, getRendererFactory())
    val JS_NAME_IS_NOT_ON_ALL_ACCESSORS: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_NAME_IS_NOT_ON_ALL_ACCESSORS", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val JS_NAME_PROHIBITED_FOR_NAMED_NATIVE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_NAME_PROHIBITED_FOR_NAMED_NATIVE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val JS_NAME_PROHIBITED_FOR_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_NAME_PROHIBITED_FOR_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val JS_NAME_ON_ACCESSOR_AND_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_NAME_ON_ACCESSOR_AND_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val JS_BUILTIN_NAME_CLASH: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("JS_BUILTIN_NAME_CLASH", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NAME_CONTAINS_ILLEGAL_CHARS: KtDiagnosticFactory0 = KtDiagnosticFactory0("NAME_CONTAINS_ILLEGAL_CHARS", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val JS_NAME_CLASH: KtDiagnosticFactory2<String, Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory2("JS_NAME_CLASH", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val JS_FAKE_NAME_CLASH: KtDiagnosticFactory3<String, FirBasedSymbol<*>, Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory3("JS_FAKE_NAME_CLASH", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val JS_SYMBOL_ON_TOP_LEVEL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_SYMBOL_ON_TOP_LEVEL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val JS_SYMBOL_PROHIBITED_FOR_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_SYMBOL_PROHIBITED_FOR_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Supertypes
    val WRONG_MULTIPLE_INHERITANCE: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("WRONG_MULTIPLE_INHERITANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())

    // Fun Interfaces
    val IMPLEMENTING_FUNCTION_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("IMPLEMENTING_FUNCTION_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtClassOrObject::class, getRendererFactory())

    // External
    val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE: KtDiagnosticFactory1<FirNamedFunctionSymbol> = KtDiagnosticFactory1("OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val EXTERNAL_ENUM_ENTRY_WITH_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_ENUM_ENTRY_WITH_BODY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING", WARNING, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtDeclaration::class, getRendererFactory())
    val INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING", WARNING, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val INLINE_CLASS_IN_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_CLASS_IN_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.FUNCTION_TYPE_RECEIVER, KtElement::class, getRendererFactory())
    val JS_EXTERNAL_INHERITORS_ONLY: KtDiagnosticFactory2<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>> = KtDiagnosticFactory2("JS_EXTERNAL_INHERITORS_ONLY", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtDeclaration::class, getRendererFactory())
    val JS_EXTERNAL_ARGUMENT: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("JS_EXTERNAL_ARGUMENT", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtExpression::class, getRendererFactory())

    // Export
    val WRONG_EXPORTED_DECLARATION: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("WRONG_EXPORTED_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NON_EXPORTABLE_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> = KtDiagnosticFactory2("NON_EXPORTABLE_TYPE", WARNING, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NON_CONSUMABLE_EXPORTED_IDENTIFIER: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NON_CONSUMABLE_EXPORTED_IDENTIFIER", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NAMED_COMPANION_IN_EXPORTED_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NAMED_COMPANION_IN_EXPORTED_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())

    // Dynamics
    val DELEGATION_BY_DYNAMIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATION_BY_DYNAMIC", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val PROPERTY_DELEGATION_BY_DYNAMIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_DELEGATION_BY_DYNAMIC", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SPREAD_OPERATOR_IN_DYNAMIC_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("SPREAD_OPERATOR_IN_DYNAMIC_CALL", ERROR, SourceElementPositioningStrategies.SPREAD_OPERATOR, KtElement::class, getRendererFactory())
    val WRONG_OPERATION_WITH_DYNAMIC: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("WRONG_OPERATION_WITH_DYNAMIC", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Static
    val JS_STATIC_NOT_IN_CLASS_COMPANION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_STATIC_NOT_IN_CLASS_COMPANION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, PsiElement::class, getRendererFactory())
    val JS_STATIC_ON_NON_PUBLIC_MEMBER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_STATIC_ON_NON_PUBLIC_MEMBER", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, PsiElement::class, getRendererFactory())
    val JS_STATIC_ON_CONST: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_STATIC_ON_CONST", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, PsiElement::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = FirJsErrorsDefaultMessages
}
