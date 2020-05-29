/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class CheckersComponent : FirSessionComponent {
    companion object {
        fun componentWithDefaultCheckers(): CheckersComponent {
            return CheckersComponent().apply {
                register(CommonDeclarationCheckers)
                register(CommonExpressionCheckers)
            }
        }
    }

    val declarationCheckers: DeclarationCheckers get() = _declarationCheckers
    private val _declarationCheckers = ComposedDeclarationCheckers()

    val expressionCheckers: ExpressionCheckers get() = _expressionCheckers
    private val _expressionCheckers = ComposedExpressionCheckers()

    fun register(checkers: DeclarationCheckers) {
        _declarationCheckers.register(checkers)
    }

    fun register(checkers: ExpressionCheckers) {
        _expressionCheckers.register(checkers)
    }

    fun register(checkers: FirAdditionalCheckersExtension) {
        register(checkers.declarationCheckers)
        register(checkers.expressionCheckers)
    }
}

val FirSession.checkersComponent: CheckersComponent by FirSession.sessionComponentAccessor()

/*
 * TODO: in future rename to `registerCheckersComponent` and configure
 *    exact checkers according to platforms of current session
 */
fun FirSession.registerCheckersComponent() {
    register(CheckersComponent::class, CheckersComponent.componentWithDefaultCheckers())
}

private class ComposedDeclarationCheckers : DeclarationCheckers() {
    override val declarationCheckers: List<FirBasicDeclarationChecker>
        get() = _declarationCheckers

    override val memberDeclarationCheckers: List<FirMemberDeclarationChecker>
        get() = _memberDeclarationCheckers

    override val constructorCheckers: List<FirConstructorChecker>
        get() = _constructorCheckers

    private val _declarationCheckers: MutableList<FirBasicDeclarationChecker> = mutableListOf()
    private val _memberDeclarationCheckers: MutableList<FirMemberDeclarationChecker> = mutableListOf()
    private val _constructorCheckers: MutableList<FirConstructorChecker> = mutableListOf()

    fun register(checkers: DeclarationCheckers) {
        _declarationCheckers += checkers.declarationCheckers
        _memberDeclarationCheckers += checkers.allMemberDeclarationCheckers
        _constructorCheckers += checkers.allConstructorCheckers
    }
}

private class ComposedExpressionCheckers : ExpressionCheckers() {
    override val expressionCheckers: List<FirBasicExpresionChecker>
        get() = _expressionCheckers
    override val qualifiedAccessCheckers: List<FirQualifiedAccessChecker>
        get() = _qualifiedAccessCheckers
    override val functionCallCheckers: List<FirFunctionCallChecker>
        get() = _functionCallCheckers

    private val _expressionCheckers: MutableList<FirBasicExpresionChecker> = mutableListOf()
    private val _qualifiedAccessCheckers: MutableList<FirQualifiedAccessChecker> = mutableListOf()
    private val _functionCallCheckers: MutableList<FirFunctionCallChecker> = mutableListOf()

    fun register(checkers: ExpressionCheckers) {
        _expressionCheckers += checkers.allExpressionCheckers
        _qualifiedAccessCheckers += checkers.allQualifiedAccessCheckers
        _functionCallCheckers += checkers.allFunctionCallCheckers
    }
}
