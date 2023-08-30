/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.types.coneType

object FirWasmExternalInheritanceChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        if (!declaration.symbol.isEffectivelyExternal(session)) {
            for (superTypeRef in declaration.superTypeRefs) {
                if (superTypeRef.toClassLikeSymbol(session)?.isEffectivelyExternal(session) == true) {
                    reporter.reportOn(
                        declaration.source,
                        FirWasmErrors.NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE,
                        superTypeRef.coneType,
                        context
                    )
                }
            }
        }
    }
}