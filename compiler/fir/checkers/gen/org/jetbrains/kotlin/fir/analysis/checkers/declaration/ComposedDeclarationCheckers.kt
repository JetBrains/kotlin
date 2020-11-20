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

internal class ComposedDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = _basicDeclarationCheckers
    override val memberDeclarationCheckers: Set<FirMemberDeclarationChecker>
        get() = _memberDeclarationCheckers
    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = _regularClassCheckers
    override val constructorCheckers: Set<FirConstructorChecker>
        get() = _constructorCheckers
    override val fileCheckers: Set<FirFileChecker>
        get() = _fileCheckers
    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker>
        get() = _controlFlowAnalyserCheckers
    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker>
        get() = _variableAssignmentCfaBasedCheckers

    private val _basicDeclarationCheckers: MutableSet<FirBasicDeclarationChecker> = mutableSetOf()
    private val _memberDeclarationCheckers: MutableSet<FirMemberDeclarationChecker> = mutableSetOf()
    private val _regularClassCheckers: MutableSet<FirRegularClassChecker> = mutableSetOf()
    private val _constructorCheckers: MutableSet<FirConstructorChecker> = mutableSetOf()
    private val _fileCheckers: MutableSet<FirFileChecker> = mutableSetOf()
    private val _controlFlowAnalyserCheckers: MutableSet<FirControlFlowChecker> = mutableSetOf()
    private val _variableAssignmentCfaBasedCheckers: MutableSet<AbstractFirPropertyInitializationChecker> = mutableSetOf()

    @CheckersComponentInternal
    internal fun register(checkers: DeclarationCheckers) {
        _basicDeclarationCheckers += checkers.allBasicDeclarationCheckers
        _memberDeclarationCheckers += checkers.allMemberDeclarationCheckers
        _regularClassCheckers += checkers.allRegularClassCheckers
        _constructorCheckers += checkers.allConstructorCheckers
        _fileCheckers += checkers.allFileCheckers
        _controlFlowAnalyserCheckers += checkers.controlFlowAnalyserCheckers
        _variableAssignmentCfaBasedCheckers += checkers.variableAssignmentCfaBasedCheckers
    }
}
