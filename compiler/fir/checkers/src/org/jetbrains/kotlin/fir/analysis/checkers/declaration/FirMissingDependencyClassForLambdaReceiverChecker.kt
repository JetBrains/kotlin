/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirMissingDependencyClassProxy
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.receiverType

object FirMissingDependencyClassForLambdaReceiverChecker :
    FirAnonymousFunctionChecker(MppCheckerKind.Common), FirMissingDependencyClassProxy {
    override fun check(
        declaration: FirAnonymousFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!declaration.isLambda) return
        val receiverType = declaration.receiverType ?: return

        val missingTypes = mutableSetOf<ConeKotlinType>()
        considerType(receiverType, missingTypes, context)
        reportMissingTypes(
            declaration.source, missingTypes, context, reporter,
            missingTypeOrigin = FirMissingDependencyClassProxy.MissingTypeOrigin.LambdaReceiver
        )
    }
}