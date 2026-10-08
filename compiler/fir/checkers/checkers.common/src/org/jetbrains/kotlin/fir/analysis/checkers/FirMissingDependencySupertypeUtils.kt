/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.FqName

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkMissingDependencySuperTypes(
    classifierType: ConeKotlinType?,
    source: KtSourceElement?,
    deprecationFeature: LanguageFeature? = null,
): Boolean = checkMissingDependencySuperTypes(classifierType?.toSymbol(), source, deprecationFeature)

/**
 * Checks for and reports [FirErrors.MISSING_DEPENDENCY_SUPERCLASS]
 * ([FirErrors.MISSING_DEPENDENCY_SUPERCLASS_WARNING] if [deprecatingFeature] is given and disabled).
 *
 * @return `true` if there is at least one missing supertype
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkMissingDependencySuperTypes(
    declaration: FirBasedSymbol<*>?,
    source: KtSourceElement?,
    deprecatingFeature: LanguageFeature? = null,
): Boolean {
    if (declaration !is FirClassSymbol<*>) return false

    val missingSuperTypes = context.session.missingDependencyStorage.getMissingSuperTypes(declaration)
    for (superType in missingSuperTypes) {
        val diagnostic =
            when {
                deprecatingFeature?.isDisabled() == true -> FirErrors.MISSING_DEPENDENCY_SUPERCLASS_WARNING
                else -> FirErrors.MISSING_DEPENDENCY_SUPERCLASS
            }

        reporter.reportOn(
            source,
            diagnostic,
            // superType.classId should be not null, FqName.ROOT added just for safety
            superType.classId?.asSingleFqName() ?: FqName.ROOT,
            declaration.classId.asSingleFqName(),
        )
    }

    return missingSuperTypes.isNotEmpty()
}
