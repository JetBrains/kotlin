/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics

class FirSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) : FirDiagnostic()

enum class DiagnosticKind {
    Syntax,
    ReturnNotAllowed,
    UnresolvedLabel,
    IllegalConstExpression,
    ConstructorInObject,
    DeserializationError,
    InferenceError,
    NoSupertype,
    TypeParameterAsSupertype,
    EnumAsSupertype,
    RecursionInSupertypes,
    RecursionInImplicitTypes,
    Java,
    Other
}