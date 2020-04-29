/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration

object DeclarationCheckers {
    val DECLARATIONS: List<FirDeclarationChecker<FirDeclaration>> = listOf(
        FirAnnotationClassDeclarationChecker,
        FirModifierChecker
    )
    val MEMBER_DECLARATIONS: List<FirDeclarationChecker<FirMemberDeclaration>> = DECLARATIONS + listOf(
        FirInfixFunctionDeclarationChecker,
        FirExposedVisibilityChecker
    )
    val CONSTRUCTORS: List<FirDeclarationChecker<FirConstructor>> = MEMBER_DECLARATIONS + listOf(
        FirConstructorChecker
    )
}