/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.resolveThis
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.toEffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.Root
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object ClassAnalyser {

    fun StateOfClass.classTyping(firClass1: FirClass): EffectsAndPotentials {
        // TODO: resolveSuperClasses
        val state = StateOfClass(firClass1, context, this)
        return state.firClass.declarations.fold(emptyEffsAndPots) { sum, d -> sum + state.analyseDeclaration(d) }
    }

    fun StateOfClass.fieldTyping(firProperty: FirProperty): EffectsAndPotentials =
        analyseDeclaration(firProperty)

    fun StateOfClass.methodTyping(firFunction: FirFunction): EffectsAndPotentials =
        firFunction.body?.let(::analyser) ?: emptyEffsAndPots

    fun StateOfClass.analyseDeclaration1(firDeclaration: FirDeclaration): EffectsAndPotentials {
        return caches.getOrPut(firDeclaration) {
            when (firDeclaration) {
                is FirRegularClass -> classTyping(firDeclaration)
                //                is FirConstructor(a: FirElement) {
                //                TODO()}
                is FirSimpleFunction -> methodTyping(firDeclaration)
                is FirProperty -> fieldTyping(firDeclaration)
                is FirField -> {
                    TODO()
                }
                else -> emptyEffsAndPots
            }
        }
    }

}

object Analyser {

    class ExpressionVisitor(private val stateOfClass: StateOfClass) : FirVisitor<EffectsAndPotentials, Nothing?>() {
        override fun visitElement(element: FirElement, data: Nothing?): EffectsAndPotentials = emptyEffsAndPots

        private fun FirElement.accept(): EffectsAndPotentials = accept(this@ExpressionVisitor, null)

        private fun analyseArgumentList(argumentList: FirArgumentList): EffectsAndPotentials =
            argumentList.arguments.fold(emptyEffsAndPots) { sum, argDec ->
                val (effs, pots) = argDec.accept()
                sum + effs + promote(pots)
            }

        private fun analyseQualifiedAccess(firQualifiedAccess: FirQualifiedAccess): EffectsAndPotentials = firQualifiedAccess.run {
            setOfNotNull(
                explicitReceiver, dispatchReceiver, extensionReceiver
            ).fold(emptyEffsAndPots) { sum, receiver ->
                val recEffsAndPots = receiver.accept().let {
                    if (receiver != extensionReceiver) it
                    else (promote(it.potentials) + it.effects).toEffectsAndPotentials()
                }
                sum + recEffsAndPots
            }
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Nothing?): EffectsAndPotentials {
            return emptyEffsAndPots
        }

        override fun visitAnnotation(annotation: FirAnnotation, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: Nothing?
        ): EffectsAndPotentials {
            TODO()
        }

        @OptIn(SymbolInternals::class)
        override fun visitPropertyAccessExpression(
            propertyAccessExpression: FirPropertyAccessExpression,
            data: Nothing?
        ): EffectsAndPotentials {
            val (prefEffs, prefPots) = visitQualifiedAccess(propertyAccessExpression, null)                        // Φ, Π

            val calleeReference = propertyAccessExpression.calleeReference
            return if (calleeReference is FirSuperReference)
                EffectsAndPotentials(prefEffs, listOf(Root.Super(calleeReference, stateOfClass.firClass)))
            else {
                val firProperty = calleeReference.toResolvedCallableSymbol()?.fir as FirVariable

                val effsAndPots = stateOfClass.select(prefPots, firProperty)
                prefEffs + effsAndPots                                                              // Φ ∪ Φ', Π'
            }
        }

        @OptIn(SymbolInternals::class)
        override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): EffectsAndPotentials {
            val (prefEffs, prefPots) = visitQualifiedAccess(functionCall, null)

            val dec = functionCall.calleeReference.toResolvedCallableSymbol()?.fir

            val effsAndPotsOfMethod =
                when (dec) {
                    is FirAnonymousFunction -> TODO()
                    is FirConstructor ->
                        dec.getClassFromConstructor()?.let { init(prefPots, it) } ?: TODO()
                    is FirErrorFunction -> TODO()
                    is FirPropertyAccessor -> stateOfClass.select(prefPots, dec.propertySymbol?.fir!!)
                    is FirSimpleFunction -> stateOfClass.call(prefPots, dec)
                    is FirBackingField -> TODO()
                    is FirEnumEntry -> TODO()
                    is FirErrorProperty -> TODO()
                    is FirField -> TODO()
                    is FirProperty -> TODO()
                    is FirValueParameter -> TODO()
                    null -> emptyEffsAndPots
                }

            val (effsOfArgs, _) = functionCall.argumentList.accept()
            // TODO: explicit receiver promotion
            return effsAndPotsOfMethod + effsOfArgs + prefEffs
        }

        override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitCallableReferenceAccess(
            callableReferenceAccess: FirCallableReferenceAccess,
            data: Nothing?
        ): EffectsAndPotentials {
            TODO()
        }

        @OptIn(SymbolInternals::class)
        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Nothing?): EffectsAndPotentials {
            val firThisReference = thisReceiverExpression.calleeReference
            val firClass = firThisReference.boundSymbol?.fir as FirClass
            val effectsAndPotentials = EffectsAndPotentials(Root.This(firThisReference, firClass))
            return if (stateOfClass.superClasses.contains(firClass) || firClass === stateOfClass.firClass)
                effectsAndPotentials else resolveThis(firClass, effectsAndPotentials, stateOfClass)
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

        override fun visitWhileLoop(whileLoop: FirWhileLoop, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

        override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Nothing?): EffectsAndPotentials {
            TODO()
        }

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
            TODO()
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
            stateOfClass.errors.addAll(effs.flatMap(stateOfClass::effectChecking))

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
            val effsAndPots = returnExpression.result.accept()
            return effsAndPots
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

