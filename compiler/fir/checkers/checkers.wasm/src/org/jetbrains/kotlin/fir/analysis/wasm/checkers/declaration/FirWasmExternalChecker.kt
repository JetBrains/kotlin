/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmExternalChecker : FirWebCommonExternalChecker(allowCompanionInInterface = false) {
    override fun isNativeOrEffectivelyExternal(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isEffectivelyExternal(session)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun reportExternalEnum(declaration: FirDeclaration) {
        reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "enum class")
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun additionalCheck(declaration: FirDeclaration) {
        if (declaration is FirFunction) {
            if (declaration.isInline) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION)
            }
            if (declaration.isTailRec) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "tailrec function")
            }
            if (declaration.isSuspend) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "suspend function")
            }
            if (LanguageFeature.ContextParameters.isEnabled()) {
                if (declaration.contextParameters.isNotEmpty()) {
                    reporter.reportOn(declaration.source, FirWasmErrors.EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS)
                }
            }
        }

        if (declaration is FirProperty) {
            if (declaration.isLateInit) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "lateinit property")
            }
            if (LanguageFeature.ContextParameters.isEnabled()) {
                if (declaration.contextParameters.isNotEmpty()) {
                    reporter.reportOn(declaration.source, FirWasmErrors.EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS)
                }
            }
        }
    }

    override fun isDefinedExternallyCallableId(callableId: CallableId?): Boolean =
        callableId == WebCommonStandardClassIds.Callables.JsDefinedExternally

    override fun hasExternalLikeAnnotations(declaration: FirDeclaration, session: FirSession): Boolean =
        false
}

