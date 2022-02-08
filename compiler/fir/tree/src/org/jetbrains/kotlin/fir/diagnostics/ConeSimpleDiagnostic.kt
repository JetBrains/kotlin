/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class ConeSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) : ConeDiagnostic

class ConeNotAnnotationContainer(val text: String) : ConeDiagnostic {
    override val reason: String get() = "Strange annotated expression: $text"
}

abstract class ConeDiagnosticWithSource(val source: KtSourceElement) : ConeDiagnostic

class ConeUnderscoreIsReserved(source: KtSourceElement) : ConeDiagnosticWithSource(source) {
    override val reason: String get() = "Names _, __, ___, ..., are reserved in Kotlin"
}

class ConeUnderscoreUsageWithoutBackticks(source: KtSourceElement) : ConeDiagnosticWithSource(source) {
    override val reason: String get() = "Names _, __, ___, ... can be used only in back-ticks (`_`, `__`, `___`, ...)"
}

class ConeAmbiguousSuper(val candidateTypes: List<ConeKotlinType>) : ConeDiagnostic {
    override val reason: String
        get() = "Ambiguous supertype"
}

enum class DiagnosticKind {
    Syntax,
    ExpressionExpected,
    NotLoopLabel,
    JumpOutsideLoop,
    VariableExpected,

    ReturnNotAllowed,
    UnresolvedLabel,
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
    CannotInferParameterType,
    IllegalProjectionUsage,
    MissingStdlibClass,
    NotASupertype,
    SuperNotAvailable,

    LoopInSupertype,
    RecursiveTypealiasExpansion,
    UnresolvedSupertype,
    UnresolvedExpandedType,

    IncorrectCharacterLiteral,
    EmptyCharacterLiteral,
    TooManyCharactersInCharacterLiteral,
    IllegalEscape,

    IntLiteralOutOfRange,
    FloatLiteralOutOfRange,
    WrongLongSuffix,

    IsEnumEntry,
    EnumEntryAsType,

    Other,
}
