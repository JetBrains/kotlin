/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.checkClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.Root
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object ClassAnalyser {

    fun StateOfClass.classTyping(firClass1: FirClass): EffectsAndPotentials {
        // TODO: resolveSuperClasses
        return StateOfClass(firClass1).allEffectsAndPotentials()
    }

    fun StateOfClass.fieldTyping(firProperty: FirProperty): EffectsAndPotentials =
        firProperty.initializer?.let(::analyser) ?: throw IllegalArgumentException()

    fun StateOfClass.methodTyping(firFunction: FirFunction): EffectsAndPotentials =
        firFunction.body?.let(::analyser) ?: throw IllegalArgumentException()

    fun StateOfClass.analyseDeclaration(firDeclaration: FirDeclaration): EffectsAndPotentials =
        when (firDeclaration) {
            is FirRegularClass -> classTyping(firDeclaration)
//                is FirConstructor -> TODO()
            is FirSimpleFunction -> methodTyping(firDeclaration)
            is FirProperty -> fieldTyping(firDeclaration)
            is FirField -> TODO()
            else -> EffectsAndPotentials()
        }

    fun StateOfClass.allEffectsAndPotentials(): EffectsAndPotentials =
        firClass.declarations.fold(EffectsAndPotentials()) { prev, dec ->
            val effsAndPots = analyseDeclaration(dec)
            prev + effsAndPots
        }
}

@OptIn(SymbolInternals::class)
fun StateOfClass.analyser(firElement: FirElement): EffectsAndPotentials =
    when (firElement) {
        is FirBlock -> firElement.statements.fold(EffectsAndPotentials()) { sum, firStatement ->
            sum + analyser(firStatement)
        }
        is FirTypeOperatorCall -> firElement.arguments.fold(EffectsAndPotentials()) { sum, operator ->
            sum + analyser(operator)
        }
        is FirFunctionCall -> {
            val receiver = firElement.getReceiver()
            val prefEffsAndPots = receiver?.let(::analyser) ?: EffectsAndPotentials()

            val firSimpleFunction = firElement.calleeReference.toResolvedCallableSymbol()?.fir as FirSimpleFunction

            val effsAndPotsOfMethod = call(prefEffsAndPots.potentials, firSimpleFunction)

            val effsAndPotsOfArgs = firElement.arguments.fold(EffectsAndPotentials()) { sum, argDec ->
                val (effs, pots) = analyser(argDec)
                sum + effs + promote(pots)
                // TODO: explicit receiver promotion
            }
            effsAndPotsOfMethod + effsAndPotsOfArgs + prefEffsAndPots
        }
        is FirPropertyAccessExpression -> {
            val receiver = firElement.getReceiver()

            val firProperty = firElement.calleeReference.toResolvedCallableSymbol()?.fir as FirVariable

            val (prefEffs, prefPots) = receiver?.let(::analyser) ?: EffectsAndPotentials()      // Φ, Π
            val effsAndPots = select(prefPots, firProperty)                                     // Φ', Π'
            effsAndPots + prefEffs                                                              // Φ ∪ Φ', Π'
        }
        is FirReturnExpression -> {
            analyser(firElement.result)
        }
        is FirThisReceiverExpression -> {
            val firClass = firElement.calleeReference.boundSymbol?.fir as FirClass
            resolveThis(firClass, EffectsAndPotentials(Root.This(firClass)), firClass)
        }
        is FirConstExpression<*> -> EffectsAndPotentials()  // ???
        is FirWhenBranch -> firElement.run { analyser(condition) + analyser(result) }
        is FirWhenExpression -> firElement.run {
            val effsAndPots = branches.fold(EffectsAndPotentials()) { sum, branch -> sum + analyser(branch) }
            val sub = (subject ?: subjectVariable)?.let(::analyser) ?: EffectsAndPotentials()
            sub + effsAndPots
        }
        is FirVariableAssignment -> {
            val effsAnsPots = analyser(firElement.rValue)
            when (val firDeclaration = firElement.lValue.toResolvedCallableSymbol()?.fir) {
                is FirProperty -> {
                    notFinalAssignments.getOrElse(firDeclaration) {
                        maybeUninitializedProperties[firDeclaration]
                    }
                }
                is FirVariable -> {}
                else -> throw IllegalArgumentException()
            }

            TODO()
        }
        else -> throw IllegalArgumentException()
    }

private fun FirQualifiedAccess.getReceiver(): FirExpression? = when {
    explicitReceiver != null -> explicitReceiver
    dispatchReceiver !is FirNoReceiverExpression -> dispatchReceiver
    extensionReceiver !is FirNoReceiverExpression -> extensionReceiver
    else -> null
}

fun analyseAndCheck(firClass: FirClass) {
    val checker = StateOfClass(firClass)
    val errors = checker.checkClass()
}