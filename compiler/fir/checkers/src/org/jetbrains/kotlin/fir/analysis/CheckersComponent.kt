/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class CheckersComponent : FirSessionComponent {
    val declarationCheckers: DeclarationCheckers get() = _declarationCheckers
    private val _declarationCheckers = ComposedDeclarationCheckers()

    val expressionCheckers: ExpressionCheckers get() = _expressionCheckers
    private val _expressionCheckers = ComposedExpressionCheckers()

    @SessionConfiguration
    fun register(checkers: DeclarationCheckers) {
        _declarationCheckers.register(checkers)
    }

    @SessionConfiguration
    fun register(checkers: ExpressionCheckers) {
        _expressionCheckers.register(checkers)
    }

    @SessionConfiguration
    fun register(checkers: FirAdditionalCheckersExtension) {
        register(checkers.declarationCheckers)
        register(checkers.expressionCheckers)
    }
}

val FirSession.checkersComponent: CheckersComponent by FirSession.sessionComponentAccessor()

private class ComposedDeclarationCheckers : DeclarationCheckers() {
    override val fileCheckers: List<FirFileChecker>
        get() = _fileCheckers
    override val declarationCheckers: List<FirBasicDeclarationChecker>
        get() = _declarationCheckers
    override val memberDeclarationCheckers: List<FirMemberDeclarationChecker>
        get() = _memberDeclarationCheckers
    override val regularClassCheckers: List<FirRegularClassChecker>
        get() = _regularClassCheckers
    override val constructorCheckers: List<FirConstructorChecker>
        get() = _constructorCheckers
    override val controlFlowAnalyserCheckers: List<FirControlFlowChecker>
        get() = _controlFlowAnalyserCheckers
    override val variableAssignmentCfaBasedCheckers: List<AbstractFirPropertyInitializationChecker>
        get() = _variableAssignmentCfaBasedCheckers

    private val _fileCheckers: MutableList<FirFileChecker> = mutableListOf()
    private val _declarationCheckers: MutableList<FirBasicDeclarationChecker> = mutableListOf()
    private val _memberDeclarationCheckers: MutableList<FirMemberDeclarationChecker> = mutableListOf()
    private val _regularClassCheckers: MutableList<FirRegularClassChecker> = mutableListOf()
    private val _constructorCheckers: MutableList<FirConstructorChecker> = mutableListOf()
    private val _controlFlowAnalyserCheckers: MutableList<FirControlFlowChecker> = mutableListOf()
    private val _variableAssignmentCfaBasedCheckers: MutableList<AbstractFirPropertyInitializationChecker> = mutableListOf()

    fun register(checkers: DeclarationCheckers) {
        _fileCheckers += checkers.allFileCheckers
        _declarationCheckers += checkers.declarationCheckers
        _memberDeclarationCheckers += checkers.allMemberDeclarationCheckers
        _regularClassCheckers += checkers.allRegularClassCheckers
        _constructorCheckers += checkers.allConstructorCheckers
        _controlFlowAnalyserCheckers += checkers.controlFlowAnalyserCheckers
        _variableAssignmentCfaBasedCheckers += checkers.variableAssignmentCfaBasedCheckers
    }
}

private class ComposedExpressionCheckers : ExpressionCheckers() {
    override val expressionCheckers: List<FirBasicExpresionChecker>
        get() = _expressionCheckers
    override val qualifiedAccessCheckers: List<FirQualifiedAccessChecker>
        get() = _qualifiedAccessCheckers
    override val functionCallCheckers: List<FirFunctionCallChecker>
        get() = _functionCallCheckers
    override val variableAssignmentCheckers: List<FirVariableAssignmentChecker>
        get() = _variableAssignmentCheckers

    private val _expressionCheckers: MutableList<FirBasicExpresionChecker> = mutableListOf()
    private val _qualifiedAccessCheckers: MutableList<FirQualifiedAccessChecker> = mutableListOf()
    private val _functionCallCheckers: MutableList<FirFunctionCallChecker> = mutableListOf()
    private val _variableAssignmentCheckers: MutableList<FirVariableAssignmentChecker> = mutableListOf()

    fun register(checkers: ExpressionCheckers) {
        _expressionCheckers += checkers.allExpressionCheckers
        _qualifiedAccessCheckers += checkers.allQualifiedAccessCheckers
        _functionCallCheckers += checkers.allFunctionCallCheckers
        _variableAssignmentCheckers += checkers.variableAssignmentCheckers
    }
}
