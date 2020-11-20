/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class DeclarationCheckers {
    companion object {
        val EMPTY: DeclarationCheckers = object : DeclarationCheckers() {}
    }

    open val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = emptySet()
    open val memberDeclarationCheckers: Set<FirMemberDeclarationChecker> = emptySet()
    open val regularClassCheckers: Set<FirRegularClassChecker> = emptySet()
    open val constructorCheckers: Set<FirConstructorChecker> = emptySet()
    open val fileCheckers: Set<FirFileChecker> = emptySet()

    open val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = emptySet()
    open val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicDeclarationCheckers: Set<FirBasicDeclarationChecker> get() = basicDeclarationCheckers
    @CheckersComponentInternal internal val allMemberDeclarationCheckers: Set<FirMemberDeclarationChecker> get() = memberDeclarationCheckers + allBasicDeclarationCheckers
    @CheckersComponentInternal internal val allRegularClassCheckers: Set<FirRegularClassChecker> get() = regularClassCheckers + allMemberDeclarationCheckers
    @CheckersComponentInternal internal val allConstructorCheckers: Set<FirConstructorChecker> get() = constructorCheckers + allMemberDeclarationCheckers
    @CheckersComponentInternal internal val allFileCheckers: Set<FirFileChecker> get() = fileCheckers + allBasicDeclarationCheckers
}
