/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirJvmCompanionBlockPropertyChecker : FirPropertyChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.isCompanionBlockMember || declaration.source?.kind != KtRealSourceElementKind) return

        if (declaration.getContainingClass()?.isInterface == true &&
            (declaration.hasBackingField || declaration.delegate != null) &&
            !declaration.isConst && declaration.backingField?.hasAnnotation(JvmStandardClassIds.Annotations.JvmField, context.session) != true
        ) {
            reporter.reportOn(declaration.source, FirJvmErrors.INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD)
        }
    }

    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true
}
