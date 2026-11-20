/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.js.checkers.checkJsModuleUsage
import org.jetbrains.kotlin.fir.analysis.js.checkers.superClassNotAny
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonJsModuleChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule

object FirJsModuleChecker : FirWebCommonJsModuleChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        checkSuperClass(declaration)
        super.check(declaration)
    }

    override fun FirBasedSymbol<*>.findModuleAnnotation(session: FirSession): FirAnnotation? =
        resolvedAnnotationsWithClassIds.getAnnotationByClassId(JsModule, session)
            ?: resolvedAnnotationsWithClassIds.getAnnotationByClassId(JsNonModule, session)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperClass(declaration: FirDeclaration) {
        val classDeclaration = declaration as? FirClass ?: return
        val superClassSymbol = classDeclaration.superClassNotAny(context.session)?.toSymbol() ?: return

        val superClassRef = classDeclaration.superTypeRefs.firstOrNull {
            it.coneTypeOrNull?.toSymbol() == superClassSymbol
        }
        checkJsModuleUsage(superClassSymbol, superClassRef?.source ?: declaration.source)
    }
}
