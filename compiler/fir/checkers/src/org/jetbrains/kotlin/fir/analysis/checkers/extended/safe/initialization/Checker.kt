/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoCollector
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.EffectsAndPotentials.Companion.emptyEffsAndPots
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Effect
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.FieldAccess
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.MethodAccess
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.effect.Promote
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.potential.*
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenProperties
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.anonymousInitializers
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker

object Checker {
    data class StateOfClass(val firClass: FirClass, val context: CheckerContext, val outerClassState: StateOfClass? = null) {

        data class InitializationPointInfo(val firVariables: Set<FirVariable>, val isPrimeInitialization: Boolean)

        val initializationOrder = mutableMapOf<FirExpression, InitializationPointInfo>()

        val alreadyInitializedVariable = mutableSetOf<FirVariable>()
        val effectsInProcess: MutableList<Effect> = mutableListOf()

        val localInitedProperties = LinkedHashSet<FirVariable>()
        val notFinalAssignments = mutableMapOf<FirProperty, EffectsAndPotentials>()
        val caches = mutableMapOf<FirDeclaration, EffectsAndPotentials>()

        private val overriddenMembers: Map<FirMemberDeclaration, FirMemberDeclaration> = overriddenMembers(mutableMapOf())

        @OptIn(SymbolInternals::class)
        private fun overriddenMembers(map: MutableMap<FirMemberDeclaration, FirMemberDeclaration>) =
            firClass.declarations.filterIsInstanceWithChecker(FirMemberDeclaration::isOverride).associateWithTo(map) { member ->
                val getOverriddenMembers =
                    when (member) {
                        is FirSimpleFunction -> member::overriddenFunctions
                        is FirProperty -> member::overriddenProperties
                        else -> throw IllegalArgumentException()
                    }
                getOverriddenMembers(firClass.symbol, context).associateByTo(map, FirCallableSymbol<*>::fir) { member }
                member
            }

        @OptIn(SymbolInternals::class)
        val superClasses: List<FirRegularClass> =
            firClass.superTypeRefs.filterIsInstanceWithChecker<FirResolvedTypeRef> { it.delegatedTypeRef is FirUserTypeRef }
                .mapNotNull { it.toRegularClassSymbol(context.session)?.fir }

        val declarations = (superClasses + firClass).flatMap(FirClass::declarations)

        val innerClassStates = declarations.filterIsInstanceWithChecker(FirRegularClass::isInner).associateWith { innerClass ->
            StateOfClass(innerClass, context, this)
        }

        val allProperties = declarations.filterIsInstance<FirProperty>()

        val errors = mutableListOf<Error<*>>()

        private val initializationDeclarationVisitor = InitializationDeclarationVisitor()
        val declarationVisitor: FirVisitor<EffectsAndPotentials, Nothing?> = DeclarationVisitor()

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
        }

        fun FirVariable.isPropertyInitialized(): Boolean =
            this in alreadyInitializedVariable || this in localInitedProperties

        @Suppress("UNCHECKED_CAST")
        fun <T : FirMemberDeclaration> resolveMember(potential: Potential, dec: T): T =
            if (potential is Super) dec else overriddenMembers[dec] as? T ?: dec


        fun resolveThis(
            clazz: FirRegularClass,
            effsAndPots: EffectsAndPotentials,
        ): EffectsAndPotentials {
            val innerClass = firClass
            if (clazz === innerClass || clazz in superClasses)
                return effsAndPots

            val outerSelection = effsAndPots.potentials.outerSelection(innerClass)
            return outerClassState?.resolveThis(clazz, outerSelection) ?: TODO()
        }

        @OptIn(SymbolInternals::class)
        fun resolve(dec: FirCallableDeclaration): StateOfClass =
            when (val firRegularClass = dec.dispatchReceiverType?.toRegularClassSymbol(context.session)?.fir) {
                firClass -> this
                in superClasses -> this
                in innerClassStates -> innerClassStates[firRegularClass]!!
                else -> TODO()
            }

        fun checkClass(): Errors {
            for (dec in declarations) {
                val effsAndPots = dec.accept(initializationDeclarationVisitor, null)
                val declarationInitializationErrors = effsAndPots.effects.flatMap { it.check(this) }
                if (dec is FirProperty && dec.initializer != null)
                    alreadyInitializedVariable.add(dec)

                errors.addAll(declarationInitializationErrors)
            }
            return errors
        }

        private open inner class InitializationDeclarationVisitor :
            FirDefaultVisitor<EffectsAndPotentials, Nothing?>() {
            override fun visitElement(element: FirElement, data: Nothing?): EffectsAndPotentials = emptyEffsAndPots

            override fun visitConstructor(constructor: FirConstructor, data: Nothing?): EffectsAndPotentials {
                if (constructor.isPrimary)
                    alreadyInitializedVariable.addAll(constructor.valueParameters)
                return analyseBody(constructor.body)
            }

            override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Nothing?): EffectsAndPotentials =
                analyseBody(anonymousInitializer.body)

            protected fun analyseBody(body: FirBlock?): EffectsAndPotentials =
                body?.let(::analyser) ?: emptyEffsAndPots

            override fun visitProperty(property: FirProperty, data: Nothing?): EffectsAndPotentials =
                property.initializer?.let(::analyser) ?: emptyEffsAndPots

            override fun visitRegularClass(regularClass: FirRegularClass, data: Nothing?): EffectsAndPotentials =
                StateOfClass(regularClass, context, this@StateOfClass).run {
                    errors.addAll(checkClass())
                    analyseClass()
                }

            protected fun StateOfClass.analyseClass() =
                declarations.fold(emptyEffsAndPots) { sum, dec -> sum + dec.accept(initializationDeclarationVisitor, null) }
        }

        private inner class DeclarationVisitor : InitializationDeclarationVisitor() {
            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): EffectsAndPotentials =
                analyseBody(simpleFunction.body)

            override fun visitRegularClass(regularClass: FirRegularClass, data: Nothing?): EffectsAndPotentials =
                innerClassStates[regularClass]?.analyseClass() ?: emptyEffsAndPots
        }
    }
}

typealias Errors = List<Error<*>>

sealed class Error<T : Effect>(val effect: T) {
    val trace = mutableListOf<Effect>()

    fun traceToSymbols(): List<FirBasedSymbol<*>> = trace.mapNotNull(Effect::symbol)

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
