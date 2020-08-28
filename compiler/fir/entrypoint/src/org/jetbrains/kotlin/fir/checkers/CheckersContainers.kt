/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ExtendedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExtendedExpressionCheckers
import org.jetbrains.kotlin.fir.session.FirSessionFactory

fun FirSessionFactory.FirSessionConfigurator.registerCommonCheckers() {
    useCheckers(CommonDeclarationCheckers)
    useCheckers(CommonExpressionCheckers)
}

fun FirSessionFactory.FirSessionConfigurator.registerExtendedCommonCheckers() {
    useCheckers(ExtendedExpressionCheckers)
    useCheckers(ExtendedDeclarationCheckers)
}
