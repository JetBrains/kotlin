/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.name.StandardClassIds

object FirDataClassNonPublicConstructorChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility)) {
            return
        }
        if (declaration.classKind != ClassKind.CLASS || !declaration.isData) {
            return
        }
        val primaryConstructor = declaration.primaryConstructorIfAny(context.session) ?: return
        if (primaryConstructor.visibility == Visibilities.Public) {
            return
        }
        val isAlreadyAnnotated = declaration.hasAnnotation(StandardClassIds.Annotations.ConsistentCopyVisibility, context.session) ||
                declaration.hasAnnotation(StandardClassIds.Annotations.ExposedCopyVisibility, context.session)
        if (isAlreadyAnnotated) {
            return
        }
        reporter.reportOn(primaryConstructor.source, FirErrors.DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED, context)
    }
}
