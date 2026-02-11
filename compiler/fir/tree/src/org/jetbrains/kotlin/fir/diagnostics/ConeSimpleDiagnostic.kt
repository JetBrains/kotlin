/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.name.Name

class ConeSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) : ConeDiagnostic

class ConeSyntaxDiagnostic(override val reason: String) : ConeDiagnostic

class ConeNotAnnotationContainer(val text: String) : ConeDiagnostic {
    override val reason: String get() = "Strange annotated expression: $text"
}

abstract class ConeDiagnosticWithSource(val source: KtSourceElement) : ConeDiagnostic

class ConeUnderscoreIsReserved(source: KtSourceElement) : ConeDiagnosticWithSource(source) {
    override val reason: String get() = "Names _, __, ___, ..., are reserved in Kotlin"
}

class ConeMultipleLabelsAreForbidden(source: KtSourceElement) : ConeDiagnosticWithSource(source) {
    override val reason: String get() = "Multiple labels per statement are forbidden"
}

abstract class ConeCannotInferType : ConeDiagnostic

class ConeCannotInferTypeParameterType(
    val typeParameter: FirTypeParameterSymbol,
    override val reason: String = "Cannot infer type for parameter ${typeParameter.name}"
) : ConeCannotInferType() {
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for type parameter ${typeParameter.name}"
}

class ConeCannotInferValueParameterType(
    val valueParameter: FirValueParameterSymbol?,
    reason: String? = null,
    // Currently, we use it to preserve the exact previous diagnostic VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE for top-levels.
    // By top-level, we mean any lambda outside any call, both with and without an expected type.
    val isTopLevelLambda: Boolean = false,
) : ConeCannotInferType() {
    private val _reason: String? = reason

    override val reason: String get() = _reason ?: ("Cannot infer type for parameter " + (valueParameter?.let { "${it.name}" } ?: "it"))

    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for value parameter " + (valueParameter?.let { "${it.name}" } ?: "it")
}

class ConeCannotInferReceiverParameterType(
    override val reason: String = "Cannot infer type for receiver parameter"
) : ConeCannotInferType() {
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for receiver parameter"
}

class ConeTypeVariableTypeIsNotInferred(
    val typeVariableType: ConeTypeVariableType,
    override val reason: String = "Type for ${typeVariableType.typeConstructor.debugName} is not inferred"
) : ConeCannotInferType() {
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for ${typeVariableType.typeConstructor.debugName}"
}

class ConeAmbiguousSuper(val candidateTypes: List<ConeKotlinType>) : ConeDiagnostic {
    override val reason: String
        get() = "Ambiguous supertype"
}

class ConeRecursiveTypeParameterDuringErasureError(val typeParameterName: Name) : ConeDiagnostic {
    override val reason: String
        get() = "self-recursive type parameter $typeParameterName"
}

object ConeDestructuringDeclarationsOnTopLevel : ConeDiagnostic {
    override val reason: String
        get() = "Destructuring declarations are only allowed for local variables/values"
}

object ConeDanglingModifierOnTopLevel : ConeDiagnostic {
    override val reason: String
        get() = "Top level declaration expected"
}

class ConeAmbiguousFunctionTypeKinds(val kinds: List<FunctionTypeKind>) : ConeDiagnostic {
    override val reason: String
        get() = "There are multiple function kinds for functional type ref"
}

object ConeUnsupportedClassLiteralsWithEmptyLhs : ConeDiagnostic {
    override val reason: String get() = "No receiver in class literal"
}

object ConeNoConstructorError : ConeDiagnostic {
    override val reason: String get() = "This type does not have a constructor"
}

object ConeNoImplicitDefaultConstructorOnExpectClass : ConeDiagnostic {
    override val reason: String get() = "No implicit default constructor on expect class"
}

object ConeContractShouldBeFirstStatement : ConeDiagnostic {
    override val reason: String get() = "Contract should be the first statement."
}

object ConeContractMayNotHaveLabel : ConeDiagnostic {
    override val reason: String get() = "Contract call may not have a label."
}

object ConeContextParameterWithDefaultValue : ConeDiagnostic {
    override val reason: String get() = "Context parameters cannot have default values"
}

object ConeUnsupportedCollectionLiteralType : ConeDiagnostic {
    override val reason: String get() = "Unsupported collection literal type"
}

class ConeCollectionLiteralAmbiguity(val candidatesWithOf: List<FirRegularClassSymbol>) : ConeDiagnostic {
    override val reason: String get() = "Ambiguous collection literal"
}

enum class DiagnosticKind {
    ExpressionExpected,
    NotLoopLabel,
    JumpOutsideLoop,
    VariableExpected,

    ReturnNotAllowed,
    UnresolvedLabel,
    AmbiguousLabel,
    LabelNameClash,
    NotAFunctionLabel,
    NoThis,
    IllegalConstExpression,
    IllegalSelector,
    NoReceiverAllowed,
    IllegalUnderscore,
    DeserializationError,
    InferenceError,
    RecursionInImplicitTypes,
    Java,
    SuperNotAllowed,
    ValueParameterWithNoTypeAnnotation,
    IllegalProjectionUsage,
    MissingStdlibClass,
    NotASupertype,
    SuperNotAvailable,
    AnnotationInWhereClause,
    MultipleAnnotationWithAllTarget,

    LoopInSupertype,
    RecursiveTypealiasExpansion,
    UnresolvedSupertype,
    UnresolvedExpandedType,

    IncorrectCharacterLiteral,
    EmptyCharacterLiteral,
    TooManyCharactersInCharacterLiteral,
    IllegalEscape,

    IntLiteralOutOfRange,
    IntLiteralWithLeadingZeros,
    FloatLiteralOutOfRange,
    WrongLongSuffix,
    UnsignedNumbersAreNotPresent,

    IsEnumEntry,
    EnumEntryAsType,

    UnderscoreWithoutRenamingInDestructuring,

    Other,
}
