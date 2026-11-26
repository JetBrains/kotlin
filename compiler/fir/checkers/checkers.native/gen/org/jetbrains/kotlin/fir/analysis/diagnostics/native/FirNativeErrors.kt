/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.native

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.PositioningStrategyKinds
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.NATIVE_DIAGNOSTICS_LIST]
 */
@Suppress("IncorrectFormatting")
object FirNativeErrors : KtDiagnosticsContainer() {
    // All
    val THROWS_LIST_EMPTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("THROWS_LIST_EMPTY", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INCOMPATIBLE_THROWS_OVERRIDE: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("INCOMPATIBLE_THROWS_OVERRIDE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INCOMPATIBLE_THROWS_INHERITED: KtDiagnosticFactory1<Collection<FirRegularClassSymbol>> = KtDiagnosticFactory1("INCOMPATIBLE_THROWS_INHERITED", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_THREAD_LOCAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_THREAD_LOCAL", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INVALID_CHARACTERS_NATIVE_ERROR: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INVALID_CHARACTERS_NATIVE_ERROR", ERROR, PositioningStrategyKinds.NAME_IDENTIFIER, getRendererFactory())
    val REDUNDANT_SWIFT_REFINEMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_SWIFT_REFINEMENT", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirRegularClassSymbol>> = KtDiagnosticFactory2("INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_OBJC_NAME", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INVALID_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_OBJC_NAME", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INVALID_OBJC_NAME_CHARS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INVALID_OBJC_NAME_CHARS", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INVALID_OBJC_NAME_FIRST_CHAR: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INVALID_OBJC_NAME_FIRST_CHAR", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val EMPTY_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("EMPTY_OBJC_NAME", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INCOMPATIBLE_OBJC_NAME_OVERRIDE: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirRegularClassSymbol>> = KtDiagnosticFactory2("INCOMPATIBLE_OBJC_NAME_OVERRIDE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_EXACT_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_EXACT_OBJC_NAME", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val MISSING_EXACT_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("MISSING_EXACT_OBJC_NAME", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val NON_LITERAL_OBJC_NAME_ARG: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_LITERAL_OBJC_NAME_ARG", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INVALID_OBJC_HIDES_TARGETS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_OBJC_HIDES_TARGETS", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INVALID_REFINES_IN_SWIFT_TARGETS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_REFINES_IN_SWIFT_TARGETS", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val SUBTYPE_OF_HIDDEN_FROM_OBJC: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUBTYPE_OF_HIDDEN_FROM_OBJC", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val CANNOT_CHECK_FOR_FORWARD_DECLARATION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("CANNOT_CHECK_FOR_FORWARD_DECLARATION", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val UNCHECKED_CAST_TO_FORWARD_DECLARATION: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UNCHECKED_CAST_TO_FORWARD_DECLARATION", WARNING, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val FORWARD_DECLARATION_AS_CLASS_LITERAL: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("FORWARD_DECLARATION_AS_CLASS_LITERAL", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE: KtDiagnosticFactory0 = KtDiagnosticFactory0("TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val PROPERTY_MUST_BE_VAR: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("PROPERTY_MUST_BE_VAR", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val MUST_NOT_HAVE_EXTENSION_RECEIVER: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("MUST_NOT_HAVE_EXTENSION_RECEIVER", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val MUST_BE_OBJC_OBJECT_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> = KtDiagnosticFactory2("MUST_BE_OBJC_OBJECT_TYPE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val MUST_BE_UNIT_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> = KtDiagnosticFactory2("MUST_BE_UNIT_TYPE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val CONFLICTING_OBJC_OVERLOADS: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("CONFLICTING_OBJC_OVERLOADS", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val INAPPLICABLE_OBJC_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_OBJC_OVERRIDE", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val NATIVE_SPECIFIC_ATOMIC: KtDiagnosticFactory1<Name> = KtDiagnosticFactory1("NATIVE_SPECIFIC_ATOMIC", WARNING, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val IDENTITY_HASH_CODE_ON_VALUE_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("IDENTITY_HASH_CODE_ON_VALUE_TYPE", WARNING, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val VARIADIC_FUNCTION_POINTERS_ARE_NOT_SUPPORTED: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("VARIADIC_FUNCTION_POINTERS_ARE_NOT_SUPPORTED", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val OVERRIDING_VARIADIC_OBJECTIVE_C_METHODS_IS_NOT_SUPPORTED: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("OVERRIDING_VARIADIC_OBJECTIVE_C_METHODS_IS_NOT_SUPPORTED", ERROR, PositioningStrategyKinds.OVERRIDE_MODIFIER, getRendererFactory())
    val CALLABLE_REFERENCES_TO_VARIADIC_C_FUNCTIONS_ARE_NOT_SUPPORTED: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("CALLABLE_REFERENCES_TO_VARIADIC_C_FUNCTIONS_ARE_NOT_SUPPORTED", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val CALLABLE_REFERENCES_TO_VARIADIC_OBJECTIVE_C_METHODS_ARE_NOT_SUPPORTED: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("CALLABLE_REFERENCES_TO_VARIADIC_OBJECTIVE_C_METHODS_ARE_NOT_SUPPORTED", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS: KtDiagnosticFactory0 = KtDiagnosticFactory0("STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("STRING_AS_C_OR_OBJC_VARIADIC_ARGUMENT", WARNING, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())
    val VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF", ERROR, PositioningStrategyKinds.DEFAULT, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = FirNativeErrorsDefaultMessages
}
