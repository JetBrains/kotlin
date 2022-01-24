/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmExpressionCheckers
import org.jetbrains.kotlin.fir.session.FirSessionFactory

fun FirSessionFactory.FirSessionConfigurator.registerCommonCheckers() {
    useCheckers(CommonDeclarationCheckers)
    useCheckers(CommonExpressionCheckers)
    useCheckers(CommonTypeCheckers)
}

fun FirSessionFactory.FirSessionConfigurator.registerExtendedCommonCheckers() {
    useCheckers(ExtendedExpressionCheckers)
    useCheckers(ExtendedDeclarationCheckers)
    useCheckers(ExtendedTypeCheckers)
}

fun FirSessionFactory.FirSessionConfigurator.registerJvmCheckers() {
    useCheckers(JvmDeclarationCheckers)
    useCheckers(JvmExpressionCheckers)
}

fun FirSessionFactory.FirSessionConfigurator.registerJsCheckers() {
    useCheckers(JsDeclarationCheckers)
    useCheckers(JsExpressionCheckers)
}
