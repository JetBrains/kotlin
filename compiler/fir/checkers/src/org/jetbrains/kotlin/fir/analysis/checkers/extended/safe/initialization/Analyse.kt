/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.checkClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.Root
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object ClassAnalyser {

    fun FirClass.classTyping(firClass1: FirClass): EffectsAndPotentials {
        // TODO: resolveSuperClasses
        return firClass1.allEffectsAndPotentials()
    }

    fun FirClass.fieldTyping(firProperty: FirProperty): EffectsAndPotentials =
        firProperty.initializer?.let(::analyser) ?: throw IllegalArgumentException()

    fun FirClass.methodTyping(firFunction: FirFunction): EffectsAndPotentials =
        firFunction.body?.let(::analyser) ?: throw IllegalArgumentException()

    fun FirClass.analyseDeclaration(firDeclaration: FirDeclaration): EffectsAndPotentials =
        when (firDeclaration) {
            is FirRegularClass -> classTyping(firDeclaration)
//                is FirConstructor -> TODO()
            is FirSimpleFunction -> methodTyping(firDeclaration)
            is FirProperty -> fieldTyping(firDeclaration)
            is FirField -> TODO()
            else -> EffectsAndPotentials()
        }

    fun FirClass.allEffectsAndPotentials(): EffectsAndPotentials =
        declarations.fold(EffectsAndPotentials()) { prev, dec ->
            val effsAndPots = analyseDeclaration(dec)
            prev + effsAndPots
        }
}

@OptIn(SymbolInternals::class)
fun FirClass.analyser(firExpression: FirStatement): EffectsAndPotentials =
    when (firExpression) {
        is FirBlock -> firExpression.statements.fold(EffectsAndPotentials()) { sum, firStatement ->
            sum + analyser(firStatement)
        }
        is FirTypeOperatorCall -> firExpression.arguments.fold(EffectsAndPotentials()) { sum, operator ->
            sum + analyser(operator)
        }
        is FirFunctionCall -> {
            val receiver = firExpression.getReceiver()
            val prefEffsAndPots = analyser(receiver)

            val firSimpleFunction = firExpression.calleeReference.toResolvedCallableSymbol()?.fir as FirSimpleFunction

            val effsAndPotsOfMethod = call(prefEffsAndPots.potentials, firSimpleFunction)

            val effsAndPotsOfArgs = firExpression.arguments.fold(EffectsAndPotentials()) { sum, argDec ->
                val (effs, pots) = analyser(argDec)
                sum + effs + promote(pots)
                // TODO: explicit receiver promotion
            }
            effsAndPotsOfMethod + effsAndPotsOfArgs + prefEffsAndPots
        }
        is FirPropertyAccessExpression -> {
            val receiver = firExpression.getReceiver()

            val firProperty = firExpression.calleeReference.toResolvedCallableSymbol()?.fir as FirProperty

            val (prefEffs, prefPots) = analyser(receiver)       // Φ, Π
            val effsAndPots = select(prefPots, firProperty)     // Φ', Π'
            effsAndPots + prefEffs                              // Φ ∪ Φ', Π'
        }
        is FirReturnExpression -> {
            analyser(firExpression.result)
        }
        is FirThisReceiverExpression -> {
            val firClass = firExpression.calleeReference.boundSymbol?.fir as FirClass
            resolveThis(this, EffectsAndPotentials(Root.This(firClass)), firClass)
        }
        is FirConstExpression<*> -> EffectsAndPotentials()  // ???
        else -> throw IllegalArgumentException()
    }

private fun FirQualifiedAccess.getReceiver(): FirExpression = when {
    explicitReceiver != null -> explicitReceiver!!
    dispatchReceiver !is FirNoReceiverExpression -> dispatchReceiver
    extensionReceiver !is FirNoReceiverExpression -> extensionReceiver
    else -> throw IllegalArgumentException("No receiver")
}

fun analyseAndCheck(firClass: FirClass) {
    val checker = Checker.StateOfClass(firClass)
    val errors = checker.checkClass()
}


