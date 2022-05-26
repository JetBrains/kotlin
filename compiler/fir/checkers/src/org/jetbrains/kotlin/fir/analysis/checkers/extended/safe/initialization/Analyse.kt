/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.checkClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.effectChecking
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.resolveThis
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.Root
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals

object ClassAnalyser {

    fun StateOfClass.classTyping(firClass1: FirClass): EffectsAndPotentials {
        // TODO: resolveSuperClasses
        val state = StateOfClass(firClass1, context, this)
        return state.firClass.declarations.fold(emptyEffsAndPots) { sum, d -> sum + state.analyseDeclaration(d) }
    }

    fun StateOfClass.fieldTyping(firProperty: FirProperty): EffectsAndPotentials =
        caches[firProperty] ?: analyseDeclaration(firProperty)

    fun StateOfClass.methodTyping(firFunction: FirFunction): EffectsAndPotentials =
        firFunction.body?.let(::analyser) ?: throw IllegalArgumentException()

    fun StateOfClass.analyseDeclaration1(firDeclaration: FirDeclaration): EffectsAndPotentials =
        when (firDeclaration) {
            is FirRegularClass -> classTyping(firDeclaration)
//                is FirConstructor -> TODO()
            is FirSimpleFunction -> methodTyping(firDeclaration)
            is FirProperty -> fieldTyping(firDeclaration)
            is FirField -> TODO()
            else -> emptyEffsAndPots
        }

    fun StateOfClass.allEffectsAndPotentials(): EffectsAndPotentials =
        firClass.declarations.fold(emptyEffsAndPots) { prev, dec ->
            val effsAndPots = analyseDeclaration1(dec)
            prev + effsAndPots
        }
}

fun FirClass.findDec(firDeclaration: FirDeclaration?): Boolean = declarations.contains(firDeclaration)

@OptIn(SymbolInternals::class)
fun StateOfClass.analyser(firElement: FirElement): EffectsAndPotentials =
    when (firElement) {
        is FirBlock -> firElement.statements.fold(emptyEffsAndPots) { sum, firStatement ->
            sum + analyser(firStatement)
        }
        is FirTypeOperatorCall -> firElement.arguments.fold(emptyEffsAndPots) { sum, operator ->
            sum + analyser(operator)
        }
        is FirFunctionCall -> {
            val (prefEffs, prefPots) = analyseReceivers(firElement)

            val dec = firElement.calleeReference.toResolvedCallableSymbol()?.fir

            val effsAndPotsOfMethod =
                when (dec) {
                    is FirAnonymousFunction -> TODO()
                    is FirConstructor ->
                        dec.getClassFromConstructor()?.let { init(prefPots, it) } ?: TODO()
                    is FirErrorFunction -> TODO()
                    is FirPropertyAccessor -> select(prefPots, dec.propertySymbol?.fir!!)
                    is FirSimpleFunction -> call(prefPots, dec)
                    is FirBackingField -> TODO()
                    is FirEnumEntry -> TODO()
                    is FirErrorProperty -> TODO()
                    is FirField -> TODO()
                    is FirProperty -> TODO()
                    is FirValueParameter -> TODO()
                    null -> emptyEffsAndPots
                }

            val (effsOfArgs, _) = firElement.arguments.fold(emptyEffsAndPots) { sum, argDec ->
                val (effs, pots) = analyser(argDec)
                sum + effs + promote(pots)
                // TODO: explicit receiver promotion
            }
            effsAndPotsOfMethod + effsOfArgs + prefEffs
        }
        is FirPropertyAccessExpression -> {
            val firProperty = firElement.calleeReference.toResolvedCallableSymbol()?.fir as FirVariable

            val (prefEffs, prefPots) = analyseReceivers(firElement)                        // Φ, Π

            val effsAndPots = select(prefPots, firProperty)
            prefEffs + effsAndPots                                                              // Φ ∪ Φ', Π'
        }
        is FirReturnExpression -> analyser(firElement.result)
        is FirThisReceiverExpression -> {
            val firClass = firElement.calleeReference.boundSymbol?.fir as FirClass
            resolveThis(firClass, EffectsAndPotentials(Root.This(firElement.calleeReference)), this)
        }
        is FirConstExpression<*> -> emptyEffsAndPots  // ???
        is FirWhenBranch -> firElement.run {
            val localSize = localInitedProperties.size
            val effsAndPots = analyser(condition) + analyser(result)

            var i = 0
            localInitedProperties.removeIf { i++ >= localSize }

            effsAndPots

        }
        is FirWhenExpression -> firElement.run {
            val effsAndPots = branches.fold(emptyEffsAndPots) { sum, branch -> sum + analyser(branch) }
            val sub = (subject ?: subjectVariable)?.let(::analyser) ?: emptyEffsAndPots

            val (initedFirProperties, isPrimeInitialization) = initializationOrder.getOrElse(firElement) { return sub + effsAndPots }

            if (isPrimeInitialization) {
                alreadyInitializedVariable.addAll(initedFirProperties)
//                initedFirProperties.forEach {
//                    caches[it] = notFinalAssignments[it] ?: throw java.lang.IllegalArgumentException()
//                    notFinalAssignments.remove(it)
//                }
                localInitedProperties.removeIf { initedFirProperties.contains(it) }
            } else
                localInitedProperties.addAll(initedFirProperties)

            notFinalAssignments.keys.removeIf {
                !(localInitedProperties.contains(it) || alreadyInitializedVariable.contains(it))
            }

            sub + effsAndPots
        }
        is FirVariableAssignment -> {
            val (effs, pots) = analyser(firElement.rValue)
            errors.addAll(effs.flatMap { effectChecking(it) })

            when (val firDeclaration = firElement.lValue.toResolvedCallableSymbol()?.fir) {
                is FirProperty -> {
                    val prevEffsAndPots = notFinalAssignments.getOrDefault(firDeclaration, emptyEffsAndPots)

                    localInitedProperties.add(firDeclaration)

                    val effsAndPots = prevEffsAndPots + pots
                    notFinalAssignments[firDeclaration] = effsAndPots //
                    caches[firDeclaration] = effsAndPots
                }
                is FirVariable -> {}
                else -> throw IllegalArgumentException()
            }
            emptyEffsAndPots
        }
        is FirElseIfTrueCondition -> emptyEffsAndPots
        is FirNoReceiverExpression -> emptyEffsAndPots
        else -> throw IllegalArgumentException()
    }

@OptIn(LookupTagInternals::class)
private fun FirConstructor.getClassFromConstructor() =
    containingClass()?.toFirRegularClass(moduleData.session)

fun StateOfClass.analyseDeclaration(dec: FirDeclaration): EffectsAndPotentials {
    val effsAndPots = when (dec) {
        is FirConstructor -> dec.body?.let(::analyser) ?: emptyEffsAndPots
        is FirAnonymousInitializer -> dec.body?.let(::analyser) ?: emptyEffsAndPots
        is FirRegularClass -> {
            val state = StateOfClass(dec, context, this)
            this.errors.addAll(state.checkClass())
            state.firClass.declarations.fold(emptyEffsAndPots) { sum, d -> sum + state.analyseDeclaration(d) }
        }
        is FirPropertyAccessor -> TODO()
        //                is FirSimpleFunction -> checkBody(dec)
        is FirField -> TODO()
        is FirProperty -> dec.initializer?.let(::analyser) ?: emptyEffsAndPots
        else -> emptyEffsAndPots
    }
    return effsAndPots
}

private fun StateOfClass.analyseReceivers(firQualifiedAccess: FirQualifiedAccess): EffectsAndPotentials = firQualifiedAccess.run {
    setOfNotNull(
        explicitReceiver,
        dispatchReceiver,
        extensionReceiver
    ).fold(emptyEffsAndPots) { effsAndPots, receiver -> effsAndPots + analyser(receiver) }
}