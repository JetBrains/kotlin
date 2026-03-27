/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationClassForOptInMarker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol

object FirOptInMarkedDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        for (annotation in declaration.annotations) {
            if (annotation.getAnnotationClassForOptInMarker(context.session) == null) continue

            val useSiteTarget = annotation.useSiteTarget
            if (declaration is FirPropertyAccessor && declaration.isGetter) {
                reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "getter")
            }
            if (declaration is FirValueParameter) {
                reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "parameter")
            }
            if (declaration is FirProperty && declaration.symbol is FirLocalPropertySymbol) {
                reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "variable")
            }
            if (declaration is FirBackingField || useSiteTarget == PROPERTY_DELEGATE_FIELD) {
                reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "field")
            }
        }

        if (declaration !is FirCallableDeclaration) return
        val receiver = declaration.receiverParameter ?: return
        for (annotation in receiver.annotations) {
            if (annotation.getAnnotationClassForOptInMarker(context.session) != null) {
                reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "parameter")
            }
        }
    }
}
