/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.forEachSupertypeWithInheritor
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType

object FirAnnotationClassInheritanceChecker : FirClassChecker(MppCheckerKind.Common) {
    @SymbolInternals
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val originatingImmediateSupertypeSource = declaration.superTypeRefs
            .mapNotNull { it.coneType.toClassLikeSymbol()?.to(it.source) }
            .toMap().toMutableMap()

        declaration.symbol.forEachSupertypeWithInheritor(
            deep = true,
            lookupInterfaces = true,
            substituteSuperTypes = true,
            supertypeSupplier = SupertypeSupplier.Default,
            useSiteSession = context.session,
        ) { supertype, inheritor ->
            val superSymbol = supertype.toClassLikeSymbol()
                ?: return@forEachSupertypeWithInheritor
            originatingImmediateSupertypeSource.putIfAbsent(superSymbol, originatingImmediateSupertypeSource[inheritor])

            if (superSymbol !is FirRegularClassSymbol || superSymbol.classKind != ClassKind.ANNOTATION_CLASS) {
                return@forEachSupertypeWithInheritor
            }
            val source = when {
                inheritor == declaration.symbol -> originatingImmediateSupertypeSource[superSymbol]
                else -> declaration.source
            }
            reporter.reportOn(source, FirErrors.EXTENDING_AN_ANNOTATION_CLASS, superSymbol)
        }
    }
}
