/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.types.FirTypeRef

// We don't declare it as `in T` because we want to prevent accidentally adding more general checkers to sets of specific checkers.
abstract class FirTypeChecker<T : FirTypeRef>(final override val mppKind: MppCheckerKind) : FirCheckerWithMppKind {
    /**
     * [FirTypeChecker] should only be used when the check can be performed independent of the context of the type refs. That is,
     * you should NOT be examining containing declarations, qualified accesses, etc. when writing a FirTypeChecker.
     *
     * If the check is dependent on context, or if it is specific to type refs in a certain kind of declaration or expression,
     * please write a [org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker] or
     * [org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker] instead.
     */
    @OptIn(DeprecatedForRemovalCompilerApi::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun check(typeRef: T) {
        check(typeRef, context, reporter)
    }

    @DeprecatedForRemovalCompilerApi(CompilerVersionOfApiDeprecation._2_2_0)
    open fun check(typeRef: T, context: CheckerContext, reporter: DiagnosticReporter) {
        throw NotImplementedError("Neither overload of 'check' was overridden.")
    }
}
