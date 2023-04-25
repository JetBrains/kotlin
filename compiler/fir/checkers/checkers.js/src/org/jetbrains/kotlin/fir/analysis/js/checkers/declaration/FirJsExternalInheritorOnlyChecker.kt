/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExternalInheritorsOnly
import org.jetbrains.kotlin.utils.addToStdlib.popLast

object FirJsExternalInheritorOnlyChecker : FirClassChecker() {
    private fun FirClass.forEachParents(context: CheckerContext, f: (FirRegularClassSymbol) -> Unit) {
        val todo = superConeTypes.toMutableList()
        val done = hashSetOf<FirRegularClassSymbol>()

        while (todo.isNotEmpty()) {
            val classSymbol = todo.popLast().toRegularClassSymbol(context.session) ?: continue
            if (done.add(classSymbol)) {
                f(classSymbol)
                classSymbol.resolvedSuperTypeRefs.mapNotNullTo(todo) { it.type as? ConeClassLikeType }
            }
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isEffectivelyExternal(context)) {
            declaration.forEachParents(context) { parent ->
                if (parent.hasAnnotation(JsExternalInheritorsOnly, context.session)) {
                    reporter.reportOn(
                        declaration.source,
                        FirJsErrors.JS_EXTERNAL_INHERITORS_ONLY,
                        parent,
                        declaration.symbol,
                        context
                    )
                }
            }
        }
    }
}
