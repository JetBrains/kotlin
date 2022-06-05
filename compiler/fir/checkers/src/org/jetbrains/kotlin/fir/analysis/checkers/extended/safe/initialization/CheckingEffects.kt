/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoCollector
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.toEffectsAndPotentials
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Potential.*
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization._Effect.*
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenProperties
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.anonymousInitializers
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker

class CheckingEffects {
}

object Checker {

    fun resolveThis(
        clazz: FirClass,
        effsAndPots: EffectsAndPotentials,
        stateOfClass: StateOfClass,
    ): EffectsAndPotentials {
        val innerClass = stateOfClass.firClass
        if (clazz === innerClass || stateOfClass.superClasses.contains(clazz))
            return effsAndPots

        val outerSelection = outerSelection(effsAndPots.potentials, innerClass)
        // val outerClass =  // outerClass for innerClass
        return stateOfClass.outerClassState?.let { resolveThis(clazz, outerSelection, it) } ?: TODO()
    }

    @OptIn(SymbolInternals::class)
    fun resolve(dec: FirCallableDeclaration): StateOfClass =
        dec.dispatchReceiverType?.toRegularClassSymbol(dec.moduleData.session)?.fir?.let(cache::get) ?: TODO()

    val cache = mutableMapOf<FirClass, StateOfClass>()

    @OptIn(SymbolInternals::class)
    data class StateOfClass(val firClass: FirClass, val context: CheckerContext, val outerClassState: StateOfClass? = null) {
        val thisPot = Root.This(firClass)

        fun FirVariable.isPropertyInitialized(): Boolean =
            alreadyInitializedVariable.contains(this) || localInitedProperties.contains(this)

        val alreadyInitializedVariable = mutableSetOf<FirVariable>()

        data class InitializationPointInfo(val firVariables: Set<FirVariable>, val isPrimeInitialization: Boolean)

        val initializationOrder = mutableMapOf<FirExpression, InitializationPointInfo>()
        val effectsInProcess = mutableListOf<Effect>()

        val localInitedProperties = LinkedHashSet<FirVariable>()
        val notFinalAssignments = mutableMapOf<FirProperty, EffectsAndPotentials>()
        val caches = mutableMapOf<FirDeclaration, EffectsAndPotentials>()

        val allProperties = firClass.declarations.filterIsInstance<FirProperty>()

        val overriddenMembers = overriddenMembers()

        private fun overriddenMembers(): Map<FirMemberDeclaration, FirMemberDeclaration> {
            val members = firClass.declarations.filterIsInstanceWithChecker(FirMemberDeclaration::isOverride)
            val map = mutableMapOf<FirMemberDeclaration, FirMemberDeclaration>()
            members.forEach { member ->
                val getOverriddenMembers = when (member) {
                    is FirSimpleFunction -> member::overriddenFunctions
                    is FirProperty -> member::overriddenProperties
                    else -> throw IllegalArgumentException()
                }
                getOverriddenMembers(firClass.symbol, context).associateByTo(map, FirCallableSymbol<*>::fir) { member }
                map[member] = member
            }
            return map
        }

        val superClasses =
            firClass.superTypeRefs.filterIsInstanceWithChecker<FirResolvedTypeRef> { it.delegatedTypeRef is FirUserTypeRef }
                .mapNotNull { it.toRegularClassSymbol(context.session)?.fir }
        val declarations = (superClasses + firClass).flatMap { clazz -> clazz.declarations }


        val errors = mutableListOf<Error<*>>()

        @OptIn(SymbolInternals::class)
        fun FirAnonymousInitializer.initBlockAnalyser(propertySymbols: Set<FirPropertySymbol>) {
            val graph = controlFlowGraphReference?.controlFlowGraph ?: return
            val initLevel = graph.exitNode.level
            val data = PropertyInitializationInfoCollector(propertySymbols).getData(graph)

            for (entry in data.filterKeys { it is WhenExitNode }) {
                val propertyInitializationInfo = entry.value[NormalPath] ?: PropertyInitializationInfo.EMPTY              // NormalPath ?
                val assignmentPropertiesSymbols = propertyInitializationInfo.filterValues { it.isDefinitelyVisited() }

                val assignmentProperties = assignmentPropertiesSymbols.keys.map { it.fir }.toSet()
                val whenExitNode = entry.key
                initializationOrder[whenExitNode.fir as FirExpression] =
                    InitializationPointInfo(assignmentProperties, whenExitNode.level == initLevel)
            }
        }

        init {
            val inits = firClass.anonymousInitializers
            val p = allProperties.mapTo(mutableSetOf()) { it.symbol }

            for (init in inits)
                init.initBlockAnalyser(p)

            cache[firClass] = this
        }

        fun checkClass(): Errors {
            val classErrors = declarations.flatMap { dec ->
                when (dec) {
                    is FirConstructor -> {
                        if (dec.isPrimary)
                            alreadyInitializedVariable + dec.valueParameters
                    }
                }
                val effsAndPots = analyseDeclaration(dec)
                val errors = effsAndPots.effects.flatMap { effectChecking(it) }
                if (dec is FirProperty && dec.initializer != null) {
                    alreadyInitializedVariable.add(dec)
                }
//                caches.putIfAbsent(dec, effsAndPots)
                errors
            }
            errors.addAll(classErrors)
            return errors
        }

        fun potentialPropagation(potential: Potential): EffectsAndPotentials {
            return when (potential) {
                is FieldPotential -> {
                    val (pot, field) = potential
                    when (pot) {
                        is Root.This -> {                                  // P-Acc1
                            val state = resolve(field)
                            val potentials = pot.potentialsOf(state, field)
                            potentials.viewChange(pot).toEffectsAndPotentials()
                        }
                        is Root.Warm -> {                                         // P-Acc2
                            val state = resolve(field)
                            val potentials = pot.potentialsOf(state, field)
                            potentials.viewChange(pot).toEffectsAndPotentials()
                        }
                        is Root.Cold -> EffectsAndPotentials(Promote(pot)) // or exception or empty list
                        is Root.Super -> TODO()
                        is FunPotential -> throw IllegalArgumentException()
                        else -> {                                                       // P-Acc3
                            val (effects, potentials) = potentialPropagation(pot)
                            val select = select(potentials, field)
                            select + effects
                        }
                    }
                }
                is MethodPotential -> {
                    val (pot, method) = potential
                    when (pot) {
                        is Root.This -> {                                     // P-Inv1
                            val state = resolve(method)
                            val potentials = pot.potentialsOf(state, method)
                            potentials.toEffectsAndPotentials()
                        }
                        is Root.Warm -> {                                     // P-Inv2
                            val state = resolve(method)
                            val potentials = pot.potentialsOf(state, method)  // find real state
                            potentials.viewChange(pot).toEffectsAndPotentials()
                        }
                        is Root.Cold -> EffectsAndPotentials(Promote(pot))
                        is Root.Super -> {
                            TODO()
                        }
                        is FunPotential -> TODO()
                        else -> {                                                       // P-Inv3
                            val (effects, potentials) = potentialPropagation(pot)
                            val call = call(potentials, method)
                            call + effects
                        }
                    }
                }
                is OuterPotential -> {
                    val (pot, outer) = potential
                    when (pot) {
                        is Root.This -> emptyEffsAndPots                // P-Out1
                        is Root.Warm -> {                               // P-Out2
                            val (firClass, outerPot) = pot
                            return EffectsAndPotentials(outerPot)
                            // TODO:
                            //  if (firClass != this.firClass) rec: findParent(firClass)
                            //  просто вверх по цепочке наследования если inner от кого-то наследуется
                        }
                        is Root.Cold -> EffectsAndPotentials(Promote(pot))  // or exception or empty list
                        is Root.Super -> TODO()
                        is FunPotential -> throw IllegalArgumentException()
                        else -> {                                                       // P-Out3
                            val (effects, potentials) = potentialPropagation(pot)
                            val (_, outPots) = outerSelection(potentials, outer)
                            EffectsAndPotentials(effects, outPots)
                        }
                    }
                }
                is FunPotential -> TODO() // invoke?
                is Root -> EffectsAndPotentials(potential)
            }
        }

        fun effectChecking(effect: Effect): Errors {
            if (effectsInProcess.contains(effect)) return emptyList()
            effectsInProcess.add(effect)
            val lastEffect = effectsInProcess.lastIndex
            val errors = when (effect) {
                is FieldAccess -> {
                    val (pot, field) = effect
                    when (pot) {
                        is Root.This -> {                                     // C-Acc1
                            if (field.isPropertyInitialized())
                                emptyList()
                            else listOf(Error.AccessError(effect))
                        }
                        is Root.Warm -> emptyList()                              // C-Acc2
                        is Root.Cold -> listOf(Error.AccessError(effect))           // illegal
                        is Root.Super -> TODO()
                        is FunPotential -> throw Exception()                  // impossible
                        else ->                                                         // C-Acc3
                            ruleAcc3(potentialPropagation(pot)) { p -> FieldAccess(p, field) }
                    }
                }
                is MethodAccess -> {
                    val (pot, method) = effect
                    when (pot) {
                        is Root.This -> {                                     // C-Inv1
                            val state = resolve(method)
                            val effectsOf = pot.effectsOf(state, method)
                            effectsOf.flatMap(::effectChecking)
                        }
                        is Root.Warm -> {                                     // C-Inv2
                            val state = resolve(method)
                            pot.effectsOf(state, method).flatMap { eff ->
                                viewChange(eff, pot)
                                effectChecking(eff)
                            }
                        }
                        is FunPotential -> emptyList() // invoke
                        is Root.Cold -> listOf(Error.InvokeError(effect))              // illegal
                        is Root.Super -> TODO()
                        else ->                                                         // C-Inv3
                            ruleAcc3(potentialPropagation(pot)) { p -> MethodAccess(p, method) }
                    }
                }
                is Init -> {                                                     // C-Init
                    val (pot, clazz) = effect
                    val effects = pot.effectsOf(this, clazz)
                    effects.flatMap { eff ->
                        val eff1 = viewChange(eff, pot)
                        effectChecking(eff1)
                    }
                    // ???
                }
                is Promote -> {
                    val pot = effect.potential
                    when (pot) {
                        is Root.Warm -> {                                     // C-Up1
                            pot.clazz.declarations.map {
                                when (it) {
//                                is FirAnonymousInitializer -> TODO()
                                    is FirRegularClass -> TODO()
//                                is FirConstructor -> TODO()
                                    is FirSimpleFunction -> TODO()
                                    is FirField, is FirProperty -> TODO()
                                    else -> throw IllegalArgumentException()
                                }
                            }
                        }
                        is Root.This -> listOf(Error.PromoteError(effect))
                        is FunPotential -> ruleAcc3(pot.effectsAndPotentials, ::Promote)
                        is Root.Cold -> listOf(Error.PromoteError(effect))
                        else -> {
                            ruleAcc3(potentialPropagation(pot), ::Promote)   // C-Up2

                        }
                    }
                }
            }
            for (error in errors) error.trace.add(effect)

            effectsInProcess.removeAt(lastEffect)

            return errors
        }

        private fun ruleAcc3(effectsAndPotentials: EffectsAndPotentials, producerOfEffects: (Potential) -> Effect): Errors =
            effectsAndPotentials.run {
                val errors = potentials.map { effectChecking(producerOfEffects(it)) } // call / select
                val effectErrors = effects.map(::effectChecking)
                (errors + effectErrors).flatten()
            }
    }
}
typealias Errors = List<Error<*>>

sealed class Error<T : Effect>(val effect: T) {
    val trace = mutableListOf<Effect>()

    class AccessError(effect: FieldAccess) : Error<FieldAccess>(effect) {
        override fun toString(): String {
            return "AccessError(property=${effect.field})"
        }
    }

    class InvokeError(effect: MethodAccess) : Error<MethodAccess>(effect) {
        override fun toString(): String {
            return "InvokeError(method=${effect.method})"
        }
    }

    class PromoteError(effect: Promote) : Error<Promote>(effect) {
        override fun toString(): String {
            return "PromoteError(potential=${effect.potential})"
        }
    }
}
