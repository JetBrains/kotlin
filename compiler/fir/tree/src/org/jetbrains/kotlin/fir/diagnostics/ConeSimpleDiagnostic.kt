/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

class ConeSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) : ConeDiagnostic()

class ConeNotAnnotationContainer(val text: String) : ConeDiagnostic() {
    override val reason: String get() = "Strange annotated expression: $text"
}

enum class DiagnosticKind {
    Syntax,
    ExpressionExpected,
    NotLoopLabel,
    JumpOutsideLoop,
    VariableExpected,

    ReturnNotAllowed,
    UnresolvedLabel,
    NoThis,
    IllegalConstExpression,
    IllegalSelector,
    IllegalUnderscore,
    DeserializationError,
    InferenceError,
    EnumAsSupertype,
    RecursionInSupertypes,
    RecursionInImplicitTypes,
    Java,
    SuperNotAllowed,
    ValueParameterWithNoTypeAnnotation,
    CannotInferParameterType,
    UnknownCallableKind,
    IllegalProjectionUsage,
    MissingStdlibClass,

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

    Other
}
