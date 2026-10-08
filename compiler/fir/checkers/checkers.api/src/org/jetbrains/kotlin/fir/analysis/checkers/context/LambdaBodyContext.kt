/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction

class LambdaBodyContext(
    val outermostLambda: FirAnonymousFunction,
)

fun createLambdaBodyContext(lambda: FirAnonymousFunction, context: CheckerContext): LambdaBodyContext {
    return context.lambdaBodyContext ?: LambdaBodyContext(lambda)
}
