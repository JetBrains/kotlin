/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Warn when a regular expect interface is actualized on JS by an external interface without using @JsNoRuntime on expect.
 */
@OptIn(SymbolInternals::class)
object FirJsActualExternalInterfaceSuggestJsNoRuntimeChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val member = (declaration as? FirMemberDeclaration)?.takeIf { it.isActual } ?: return

        val actualSymbol: FirClassLikeSymbol<*> = when (member) {
            is FirTypeAlias -> member.expandedTypeRef.coneType.abbreviatedTypeOrSelf.toClassSymbol() ?: return
            is FirClassLikeDeclaration -> member.symbol
            else -> return
        }

        val actualInterface = (actualSymbol.fir as? FirClass)?.takeIf { it.classKind.isInterface } ?: return
        val expectInterface = member.symbol.expectForActual.orEmpty()[ExpectActualMatchingCompatibility.MatchedSuccessfully]
            ?.singleOrNull()
            ?.let { it.fir as? FirClass }
            ?.takeIf { it.classKind.isInterface } ?: return

        if (actualInterface.isExternal) {
            if (!expectInterface.hasJsNoRuntime()) {
                reporter.reportOn(
                    member.source,
                    FirJsErrors.JS_ACTUAL_EXTERNAL_INTERFACE_WHILE_EXPECT_WITHOUT_JS_NO_RUNTIME,
                )
            }
        } else if (expectInterface.hasJsNoRuntime() && !actualInterface.hasJsNoRuntime()) {
            reporter.reportOn(
                member.source,
                FirJsErrors.JS_NO_RUNTIME_ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
            )
        }
    }

    context(context: CheckerContext)
    private fun FirAnnotationContainer.hasJsNoRuntime(): Boolean =
        hasAnnotation(JsStandardClassIds.Annotations.JsNoRuntime, context.session)
}
