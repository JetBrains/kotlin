/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef

class ReturnTypeCalculatorForFullBodyResolve private constructor(
    private val diagnosticKind: DiagnosticKind,
    private val reason: String,
) : ReturnTypeCalculator() {
    companion object {
        // It's actual only for local functions because simple members are being resolved at another phase.
        // Local properties are just unresolved if they are used recursively.
        val Default = ReturnTypeCalculatorForFullBodyResolve(
            DiagnosticKind.RecursionInImplicitTypes,
            "Recursion with local function"
        )
        val Contract = ReturnTypeCalculatorForFullBodyResolve(
            DiagnosticKind.InferenceError,
            "Cannot calculate return type during full-body resolution (local class/object?)"
        )
    }

    override val fakeOverrideTypeCalculator: FakeOverrideTypeCalculator
        get() = FakeOverrideTypeCalculator.Forced

    override fun tryCalculateReturnTypeOrNull(declaration: FirCallableDeclaration): FirResolvedTypeRef? {
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef
        if (declaration.origin.fromSupertypes) {
            return FakeOverrideTypeCalculator.Forced.computeReturnType(declaration)
        }

        return buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("$reason: ${declaration.render()}", diagnosticKind) }
    }
}
