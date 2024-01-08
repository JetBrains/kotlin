/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.isExperimentalMarker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol

object FirOptInImportsChecker : FirFileChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.imports.forEach { import ->
            if (import !is FirResolvedImport) return@forEach
            val source = import.source ?: return@forEach
            // 'import package.Class.Marker' is OK, but 'import package.Class.Marker.NestedClass' is not.
            val resolvedParentClassId = import.resolvedParentClassId ?: return@forEach
            val parentClassSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(resolvedParentClassId) ?: return@forEach

            when (parentClassSymbol) {
                is FirRegularClassSymbol -> parentClassSymbol.checkContainingClasses(source, context, reporter)
                is FirTypeAliasSymbol -> {
                    val expandedClassSymbol = parentClassSymbol.fullyExpandedClass(context.session) ?: return@forEach
                    expandedClassSymbol.checkContainingClasses(source, context, reporter)
                }
                else -> return@forEach
            }
        }
    }

    private tailrec fun FirClassLikeSymbol<*>.checkContainingClasses(
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (isExperimentalMarker(context.session)) {
            reporter.reportOn(source, FirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN, context)
        }
        val containingClassSymbol = this.getContainingClassLookupTag()?.toSymbol(context.session) ?: return
        containingClassSymbol.checkContainingClasses(source, context, reporter)
    }

}
