/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.emptyEffsAndPots
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.LambdaPotential
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Root
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.Super
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor

object Analyser {

    object ReferenceVisitor : FirDefaultVisitor<EffectsAndPotentials, Pair<StateOfClass, Potentials>>() {
        override fun visitElement(element: FirElement, data: Pair<StateOfClass, Potentials>): EffectsAndPotentials = emptyEffsAndPots

        override fun visitSuperReference(superReference: FirSuperReference, data: Pair<StateOfClass, Potentials>): EffectsAndPotentials {
            val (stateOfClass, pots) = data
            val superPots = pots.wrapPots { pot -> Super(superReference, stateOfClass.firClass, pot) }
            return EffectsAndPotentials(potentials = superPots)
        }

        @OptIn(SymbolInternals::class)
        override fun visitThisReference(thisReference: FirThisReference, data: Pair<StateOfClass, Potentials>): EffectsAndPotentials =
            (thisReference.boundSymbol as? FirClassSymbol<*>)?.let { EffectsAndPotentials(Root.This(thisReference, it.fir)) }
                ?: emptyEffsAndPots

        @OptIn(SymbolInternals::class)
        override fun visitResolvedNamedReference(
            resolvedNamedReference: FirResolvedNamedReference,
            data: Pair<StateOfClass, Potentials>
        ): EffectsAndPotentials {
            val (stateOfClass, prefPots) = data
            return when (val symbol = resolvedNamedReference.resolvedSymbol) {
                is FirVariableSymbol<*> -> prefPots.select(stateOfClass, symbol.fir)
                is FirConstructorSymbol -> prefPots.init(symbol)
                is FirAnonymousFunctionSymbol -> TODO()
                is FirFunctionSymbol<*> -> prefPots.call(stateOfClass, symbol.fir)
                else -> emptyEffsAndPots
            }
        }
    }

    class ExpressionVisitor(private val stateOfClass: StateOfClass) : FirDefaultVisitor<EffectsAndPotentials, Nothing?>() {
        override fun visitElement(element: FirElement, data: Nothing?): EffectsAndPotentials = emptyEffsAndPots

        private fun FirElement.accept(): EffectsAndPotentials = accept(this@ExpressionVisitor, null)

        private fun analyseArgumentList(argumentList: FirArgumentList): EffectsAndPotentials =
            argumentList.arguments.fold(emptyEffsAndPots) { sum, argDec ->
                val (effs, pots) = argDec.accept()
                sum + effs + pots.promote()
            }

        private fun analyseQualifiedAccess(firQualifiedAccess: FirQualifiedAccess): EffectsAndPotentials = firQualifiedAccess.run {
            setOfNotNull(
                explicitReceiver, dispatchReceiver, extensionReceiver
            ).fold(emptyEffsAndPots) { sum, receiver ->
                val recEffsAndPots = receiver.accept().let {
                    if (receiver != extensionReceiver) it
                    else EffectsAndPotentials(it.potentials.promote() + it.effects)
                }
                sum + recEffsAndPots
            }
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: Nothing?
        ): EffectsAndPotentials {
            val (prefEffs, prefPots) = visitQualifiedAccess(qualifiedAccessExpression, null)                        // Φ, Π
            val effsAndPots = qualifiedAccessExpression.calleeReference.accept(ReferenceVisitor, stateOfClass to prefPots)
            return effsAndPots + prefEffs
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): EffectsAndPotentials {
            val effsAndPotsOfMethod = visitQualifiedAccessExpression(functionCall, null)

            val (effsOfArgs, _) = functionCall.argumentList.accept()
            // TODO: explicit receiver promotion
            return effsAndPotsOfMethod + effsOfArgs
        }

        override fun visitAnonymousFunctionExpression(
            anonymousFunctionExpression: FirAnonymousFunctionExpression,
            data: Nothing?
        ): EffectsAndPotentials {

            val anonymousFunction = anonymousFunctionExpression.anonymousFunction
            val effectsAndPotentials = anonymousFunction.body?.accept() ?: emptyEffsAndPots
            val lambdaPot = LambdaPotential(effectsAndPotentials, anonymousFunction)
            return EffectsAndPotentials(lambdaPot)
        }

        override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Nothing?): EffectsAndPotentials =
            throwExpression.exception.accept()

        @OptIn(SymbolInternals::class)
        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Nothing?): EffectsAndPotentials {
            val firThisReference = thisReceiverExpression.calleeReference
            val firRegularClass = firThisReference.boundSymbol?.fir as? FirRegularClass ?: TODO()
            val effectsAndPotentials = firThisReference.accept(ReferenceVisitor, stateOfClass to EmptyPotentials)
            return stateOfClass.resolveThis(firRegularClass, effectsAndPotentials)
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Nothing?): EffectsAndPotentials {
            return whenExpression.run {
                val effsAndPots = branches.fold(emptyEffsAndPots) { sum, branch -> sum + branch.accept() }
                val sub = (subject ?: subjectVariable)?.accept() ?: emptyEffsAndPots

                val (initedFirProperties, isPrimeInitialization) = stateOfClass.initializationOrder.getOrElse(whenExpression) { return sub + effsAndPots }

                if (isPrimeInitialization) {
                    stateOfClass.alreadyInitializedVariable.addAll(initedFirProperties)
//                initedFirProperties.forEach {
//                    caches[it] = notFinalAssignments[it] ?: throw java.lang.IllegalArgumentException()
//                    notFinalAssignments.remove(it)
//                }
                    stateOfClass.localInitedProperties.removeIf(initedFirProperties::contains)
                } else
                    stateOfClass.localInitedProperties.addAll(initedFirProperties)

                stateOfClass.notFinalAssignments.keys.removeIf {
                    !(stateOfClass.localInitedProperties.contains(it) || stateOfClass.alreadyInitializedVariable.contains(it))
                }

                sub + effsAndPots
            }
        }

        override fun visitWhenBranch(whenBranch: FirWhenBranch, data: Nothing?): EffectsAndPotentials {
            return whenBranch.run {
                val localSize = stateOfClass.localInitedProperties.size
                val effsAndPots = condition.accept().effects + result.accept()

                var i = 0
                stateOfClass.localInitedProperties.removeIf { i++ >= localSize }

                effsAndPots
            }
        }

        override fun visitLoop(loop: FirLoop, data: Nothing?): EffectsAndPotentials =
            loop.run { condition.accept() + block.accept() }

        override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitStringConcatenationCall(
            stringConcatenationCall: FirStringConcatenationCall,
            data: Nothing?
        ): EffectsAndPotentials {
            TODO()
        }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitTryExpression(tryExpression: FirTryExpression, data: Nothing?): EffectsAndPotentials {
            return tryExpression.run {
                tryBlock.accept() + catches.fold(emptyEffsAndPots) { sum, cache -> sum + cache.accept() } + (finallyBlock?.accept()
                    ?: emptyEffsAndPots)
            }
        }

        override fun visitClassReferenceExpression(
            classReferenceExpression: FirClassReferenceExpression,
            data: Nothing?
        ): EffectsAndPotentials {
            TODO()
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        @OptIn(SymbolInternals::class)
        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: Nothing?): EffectsAndPotentials {
            val (effs, pots) = variableAssignment.rValue.accept()
            stateOfClass.errors.addAll(effs.flatMap { it.check(stateOfClass) })

            when (val firDeclaration = variableAssignment.lValue.toResolvedCallableSymbol()?.fir) {
                is FirProperty -> {
                    val prevEffsAndPots = stateOfClass.notFinalAssignments.getOrDefault(firDeclaration, emptyEffsAndPots)

                    stateOfClass.localInitedProperties.add(firDeclaration)

                    val effsAndPots = prevEffsAndPots + pots
                    stateOfClass.notFinalAssignments[firDeclaration] = effsAndPots //
                    stateOfClass.caches[firDeclaration] = effsAndPots
                }
                is FirVariable -> {}
                else -> throw IllegalArgumentException()
            }
            return emptyEffsAndPots
        }

        override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Nothing?): EffectsAndPotentials {
            return returnExpression.result.accept()
        }

        override fun visitBlock(block: FirBlock, data: Nothing?): EffectsAndPotentials =
            block.statements.fold(emptyEffsAndPots) { sum, firStatement -> sum + firStatement.accept() }

        override fun visitDelegatedConstructorCall(
            delegatedConstructorCall: FirDelegatedConstructorCall,
            data: Nothing?
        ): EffectsAndPotentials {
            TODO()
        }

        override fun visitCall(call: FirCall, data: Nothing?): EffectsAndPotentials =
            call.argumentList.accept()

        override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: Nothing?): EffectsAndPotentials =
            analyseQualifiedAccess(qualifiedAccess)

        override fun visitArgumentList(argumentList: FirArgumentList, data: Nothing?): EffectsAndPotentials =
            analyseArgumentList(argumentList)
    }
}

fun StateOfClass.analyser(firElement: FirElement): EffectsAndPotentials {
    val visitor = Analyser.ExpressionVisitor(this)
    return firElement.accept(visitor, null)
}