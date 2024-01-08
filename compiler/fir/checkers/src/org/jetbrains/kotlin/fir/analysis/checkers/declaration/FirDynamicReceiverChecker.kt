/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.DynamicExtension

object FirDynamicReceiverChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration.receiverParameter?.typeRef?.coneType is ConeDynamicType &&
            !declaration.hasAnnotation(DynamicExtension, context.session) &&
            declaration !is FirAnonymousFunction
        ) {
            reporter.reportOn(declaration.receiverParameter?.source, FirErrors.DYNAMIC_RECEIVER_NOT_ALLOWED, context)
        }
    }
}
