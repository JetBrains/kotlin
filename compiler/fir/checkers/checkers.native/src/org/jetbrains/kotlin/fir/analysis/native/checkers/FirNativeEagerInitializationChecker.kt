/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.name.ClassId

object FirNativeEagerInitializationChecker : FirPropertyChecker(MppCheckerKind.Platform) {
    private val eagerInitializationClassId = ClassId.topLevel(KonanFqNames.eagerInitialization)

    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        if (!context.isTopLevel || declaration.initializer == null) {
            reporter.reportIfHasAnnotation(declaration, eagerInitializationClassId, FirNativeErrors.INAPPLICABLE_EAGER_INITIALIZATION)
        }
    }
}
