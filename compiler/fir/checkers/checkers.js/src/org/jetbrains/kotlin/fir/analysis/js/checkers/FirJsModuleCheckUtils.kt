/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.serialization.js.ModuleKind

@NoMutableState
class FirJsModuleKind(val moduleKind: ModuleKind) : FirSessionComponent

private val FirSession.jsModuleKindComponent: FirJsModuleKind by FirSession.sessionComponentAccessor()

private val FirSession.jsModuleKind: ModuleKind
    get() = jsModuleKindComponent.moduleKind


internal fun checkJsModuleUsage(
    callee: FirBasedSymbol<*>,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    source: AbstractKtSourceElement?
) {
    val moduleKind = context.session.jsModuleKind

    val calleeSession = callee.moduleData.session
    val calleeRoot = getRootClassLikeSymbolOrSelf(callee, calleeSession)
    val calleeContainingFile = calleeRoot.getContainingFile(calleeSession)

    val callToModule = calleeRoot.getAnnotationStringParameter(JsStandardClassIds.Annotations.JsModule, calleeSession) != null ||
            calleeContainingFile?.symbol?.getAnnotationStringParameter(JsStandardClassIds.Annotations.JsModule, calleeSession) != null

    val callToNonModule = calleeRoot.hasAnnotation(JsStandardClassIds.Annotations.JsNonModule, calleeSession) ||
            calleeContainingFile?.symbol?.hasAnnotation(JsStandardClassIds.Annotations.JsNonModule, calleeSession) == true

    when (moduleKind) {
        ModuleKind.UMD -> {
            if (!callToNonModule && callToModule || callToNonModule && !callToModule) {
                reporter.reportOn(source, FirJsErrors.CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE, context)
            }
        }
        ModuleKind.PLAIN -> {
            if (!callToNonModule && callToModule) {
                reporter.reportOn(source, FirJsErrors.CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM, callee, context)
            }
        }
        else -> {
            if (!callToModule && callToNonModule) {
                reporter.reportOn(source, FirJsErrors.CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM, callee, context)
            }
        }
    }
}
