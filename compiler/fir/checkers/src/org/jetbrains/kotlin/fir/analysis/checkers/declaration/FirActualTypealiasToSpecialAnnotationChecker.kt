/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.resolve.calls.mpp.ActualTypealiasToSpecialAnnotationUtils.isAnnotationProhibitedInActualTypeAlias

internal object FirActualTypealiasToSpecialAnnotationChecker : FirTypeAliasChecker() {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return
        if (!declaration.isActual) return
        val typealiasedClassSymbol = declaration.expandedConeType?.toSymbol(context.session) ?: return
        if (typealiasedClassSymbol.classKind != ClassKind.ANNOTATION_CLASS) {
            return
        }
        val classId = typealiasedClassSymbol.classId
        if (isAnnotationProhibitedInActualTypeAlias(classId)) {
            reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION, classId, context)
        }
    }
}