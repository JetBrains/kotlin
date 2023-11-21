/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmExternalChecker : FirWebCommonExternalChecker() {
    override fun isNativeOrEffectivelyExternal(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isEffectivelyExternal(session)
    }

    override fun reportExternalEnum(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "enum class", context)
    }

    override fun additionalCheck(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirFunction) {
            if (declaration.isInline) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION, context)
            }
            if (declaration.isTailRec) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "tailrec function", context)
            }
            if (declaration.isSuspend) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "suspend function", context)
            }
        }

        if (declaration is FirProperty) {
            if (declaration.isLateInit) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "lateinit property", context)
            }
        }
    }

    override fun isDefinedExternallyCallableId(callableId: CallableId): Boolean =
        callableId == WebCommonStandardClassIds.Callables.JsDefinedExternally

    override fun hasExternalLikeAnnotations(declaration: FirDeclaration, session: FirSession): Boolean =
        false
}

