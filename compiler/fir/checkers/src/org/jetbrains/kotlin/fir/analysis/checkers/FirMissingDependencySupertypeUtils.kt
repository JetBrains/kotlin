/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*

fun checkMissingDependencySuperTypes(
    classifierType: ConeKotlinType?,
    source: KtSourceElement?,
    reporter: DiagnosticReporter,
    context: CheckerContext,
): Boolean = checkMissingDependencySuperTypes(classifierType?.toSymbol(context.session), source, reporter, context)

fun checkMissingDependencySuperTypes(
    declaration: FirBasedSymbol<*>?,
    source: KtSourceElement?,
    reporter: DiagnosticReporter,
    context: CheckerContext,
): Boolean {
    if (declaration !is FirClassSymbol<*>) return false

    val missingSuperTypes = context.session.missingDependencyStorage.getMissingSuperTypes(declaration)
    for (superType in missingSuperTypes) {
        reporter.reportOn(
            source,
            FirErrors.MISSING_DEPENDENCY_SUPERCLASS,
            superType.withArguments(emptyArray()).withNullability(ConeNullability.NOT_NULL, context.session.typeContext),
            declaration.constructType(emptyArray(), false),
            context
        )
    }

    return missingSuperTypes.isNotEmpty()
}
