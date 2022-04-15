/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.initialization

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

class ClassAnalyser(val firClass: FirClass) {

    fun classTyping(firClass1: FirClass): EffectsAndPotentials {
        // TODO: resolveSuperClasses
        return ClassAnalyser(firClass1).allEffectsAndPotentials()
    }

    fun fieldTyping(firProperty: FirProperty): EffectsAndPotentials =
        firProperty.initializer?.let(::analyser) ?: throw IllegalArgumentException()

    fun methodTyping(firFunction: FirFunction): EffectsAndPotentials =
        firFunction.body?.let(::analyser) ?: throw IllegalArgumentException()

    fun allEffectsAndPotentials(): EffectsAndPotentials =
        firClass.declarations.fold(EffectsAndPotentials()) { prev, dec ->
            val effsAndPots = when (dec) {
                is FirRegularClass -> classTyping(dec)
//                is FirConstructor -> TODO()
                is FirSimpleFunction -> methodTyping(dec)
                is FirProperty -> fieldTyping(dec)
                is FirField -> TODO()
                else -> EffectsAndPotentials()
            }
            prev.addEffectsAndPotentials(effsAndPots)
        }

}

fun fieldTyping(firProperty: FirProperty): EffectsAndPotentials =
    firProperty.initializer?.let(::analyser) ?: throw IllegalArgumentException()

fun methodTyping(firFunction: FirFunction): EffectsAndPotentials =
    firFunction.body?.let(::analyser) ?: throw IllegalArgumentException()

@OptIn(SymbolInternals::class)
fun analyser(firExpression: FirStatement): EffectsAndPotentials =
    when (firExpression) {
        is FirBlock -> firExpression.statements.fold(EffectsAndPotentials()) { sum, firStatement ->
            sum.addEffectsAndPotentials(analyser(firStatement))
        }
        is FirTypeOperatorCall -> firExpression.arguments.fold(EffectsAndPotentials()) { sum, operator ->
            sum.addEffectsAndPotentials(analyser(operator))
        }
        is FirFunctionCall -> {
            val receiver = firExpression.getReceiver()
            val prefEffsAndPots = analyser(receiver)

            val firCallableDeclaration = firExpression.calleeReference.candidateSymbol?.fir as? FirCallableDeclaration

            val effsAndPotsOfMethod = when (firCallableDeclaration) {
                is FirAnonymousFunction -> TODO()
                is FirProperty -> TODO()
                is FirPropertyAccessor -> firCallableDeclaration.propertySymbol?.fir?.let(::fieldTyping) ?: EffectsAndPotentials()
                is FirSimpleFunction -> call(prefEffsAndPots.potentials, firCallableDeclaration)
                is FirConstructor -> TODO()
//                is FirField -> TODO() ???
                else -> throw IllegalArgumentException()
            }

            val effsAndPotsOfArgs = firExpression.arguments.fold(EffectsAndPotentials()) { sum, argDec ->
                val (effs, pots) = analyser(argDec)
                sum.addEffectsAndPotentials(effs + promote(pots))
                // TODO: explicit receiver promotion
            }
            effsAndPotsOfMethod.addEffectsAndPotentials(effsAndPotsOfArgs).addEffectsAndPotentials(prefEffsAndPots)
        }
        is FirPropertyAccessExpression -> {
            val receiver = firExpression.getReceiver()

            val (prefEffs, prefPots) = analyser(receiver)

            val callable = firExpression.calleeReference.toResolvedCallableSymbol()

            when (callable) {

            }
            select(prefPots, TODO())

            TODO()
        }
        is FirReturnExpression -> {
            analyser(firExpression.result)
        }
        is FirThisReceiverExpression -> {

//            TODO:resolveThis()
            EffectsAndPotentials(Potential.Root.This(firExpression))
        }

        else -> throw IllegalArgumentException()
    }

private fun FirQualifiedAccess.getReceiver(): FirExpression {
    val rec = explicitReceiver ?: extensionReceiver
    if (rec is FirNoReceiverExpression)
        TODO()

    return rec
}


fun analyseAndCheck(firClass: FirClass) {
    val analyser = ClassAnalyser(firClass)

    val (effs, pots) = analyser.allEffectsAndPotentials()

    val all = effs + promote(pots)

    val checker = ClassInitializationState(firClass)
    val errors = all.map(checker::effectChecking)
}


