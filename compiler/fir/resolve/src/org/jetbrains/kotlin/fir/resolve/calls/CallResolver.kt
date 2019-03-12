/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processClassifiersByNameWithAction
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import java.util.*
import kotlin.collections.LinkedHashSet

class CallResolver(val typeCalculator: ReturnTypeCalculator) {

    lateinit var checkers: List<ApplicabilityChecker>
    lateinit var scopes: List<FirScope>

    fun runTowerResolver(): ApplicabilityChecker? {
        processedScopes.clear()

        val names = LinkedHashSet<Name>()

        var successChecker: ApplicabilityChecker? = null

        for ((index, scope) in scopes.asReversed().withIndex()) {
            checkers.forEach {
                it.updateNames(names)
            }
            processedScopes.add(scope)

            names.forEach { name ->
                fun process(symbol: ConeSymbol): ProcessorAction {
                    for (checker in checkers) {
                        checker.consumeCandidate(index, symbol, this)
                    }
                    return ProcessorAction.NEXT
                }

                scope.processClassifiersByNameWithAction(name, FirPosition.OTHER, ::process)
                scope.processPropertiesByName(name, ::process)
                scope.processFunctionsByName(name, ::process)
            }

            successChecker = checkers.maxBy { it.currentApplicability }

            if (successChecker?.currentApplicability == CandidateApplicability.RESOLVED) {
                break
            }

        }

        return successChecker
    }

    lateinit var session: FirSession
    val processedScopes = mutableListOf<FirScope>()

}


enum class CandidateApplicability {
    HIDDEN,
    WRONG_RECEIVER,
    PARAMETER_MAPPING_ERROR,
    INAPPLICABLE,
    SYNTHETIC_RESOLVED,
    RESOLVED
}


// TODO: Extract from this transformer
abstract class ApplicabilityChecker {

    val groupNumbers = mutableListOf<Int>()
    val candidates = mutableListOf<ConeSymbol>()


    var currentApplicability = CandidateApplicability.HIDDEN

    var expectedType: FirTypeRef? = null
    var explicitReceiverType: FirTypeRef? = null


    fun newDataSet() {
        groupNumbers.clear()
        candidates.clear()
        expectedType = null
        currentApplicability = CandidateApplicability.HIDDEN
    }

    protected open fun getApplicability(
        group: Int,
        symbol: ConeSymbol,
        resolver: CallResolver
    ): CandidateApplicability {
        val declaration = (symbol as? FirBasedSymbol<*>)?.fir
            ?: return CandidateApplicability.HIDDEN
        declaration as FirDeclaration

        if (declaration is FirCallableDeclaration) {
            if ((declaration.receiverTypeRef == null) != (explicitReceiverType == null)) return CandidateApplicability.PARAMETER_MAPPING_ERROR
        }

        return CandidateApplicability.RESOLVED
    }

    open fun consumeCandidate(group: Int, symbol: ConeSymbol, resolver: CallResolver) {
        val applicability = getApplicability(group, symbol, resolver)

        if (applicability > currentApplicability) {
            groupNumbers.clear()
            candidates.clear()
            currentApplicability = applicability
        }


        if (applicability == currentApplicability) {
            candidates.add(symbol)
            groupNumbers.add(group)
        }
    }

    abstract fun updateNames(names: LinkedHashSet<Name>)

    open fun isSuccessful(index: Int, candidate: ConeSymbol): Boolean {
        return true
    }

    open fun successCandidates(): List<ConeSymbol> {
        if (groupNumbers.isEmpty()) return emptyList()
        val result = mutableListOf<ConeSymbol>()
        var bestGroup = groupNumbers.first()
        for ((index, candidate) in candidates.withIndex()) {
            val group = groupNumbers[index]
            if (!isSuccessful(index, candidate)) continue
            if (bestGroup > group) {
                bestGroup = group
                result.clear()
            }
            if (bestGroup == group) {
                result.add(candidate)
            }
        }
        return result
    }
}

open class VariableApplicabilityChecker(val name: Name) : ApplicabilityChecker() {
    override fun updateNames(names: LinkedHashSet<Name>) {
        names.add(name)
    }

    override fun consumeCandidate(group: Int, symbol: ConeSymbol, resolver: CallResolver) {
        if (symbol !is ConeVariableSymbol) return
        if (symbol.callableId.callableName != name) return
        super.consumeCandidate(group, symbol, resolver)
    }
}

class VariableInvokeApplicabilityChecker(val variableName: Name) : FunctionApplicabilityChecker(invoke) {

    val variableChecker = object : VariableApplicabilityChecker(variableName) {
        override fun isSuccessful(index: Int, candidate: ConeSymbol): Boolean {
            return matchedProperties[index]
        }
    }
    private var matchedProperties = BitSet()
    private var lookupInvoke = false

    override fun updateNames(names: LinkedHashSet<Name>) {
        names.add(variableName)
        if (lookupInvoke) {
            names.add(name)
        }
    }

    private fun isInvokeApplicableOn(propertySymbol: ConeSymbol, invokeSymbol: ConeCallableSymbol): Boolean {
        return true //TODO: Actual type-check here
    }

    override fun getApplicability(
        group: Int,
        symbol: ConeSymbol,
        resolver: CallResolver
    ): CandidateApplicability {

        symbol as ConeCallableSymbol
        val declaration = (symbol as? FirBasedSymbol<*>)?.fir
            ?: return CandidateApplicability.HIDDEN
        declaration as FirDeclaration

        if (declaration is FirFunction) {
            if (declaration.valueParameters.size != parameterCount) return CandidateApplicability.PARAMETER_MAPPING_ERROR
        }

        var applicable = false

        fun processCandidates(candidates: Iterable<IndexedValue<ConeSymbol>>) {
            for ((index, candidate) in candidates) {
                val invokeApplicableOn = isInvokeApplicableOn(candidate, symbol)
                if (invokeApplicableOn) {
                    applicable = true
                }
                matchedProperties[index] = invokeApplicableOn

            }
        }

        if (group == -1) {
            processCandidates(listOf(variableChecker.candidates.withIndex().last()))
        } else {
            processCandidates(variableChecker.candidates.withIndex())
        }

        if (applicable) {
            return CandidateApplicability.RESOLVED
        }
        return CandidateApplicability.PARAMETER_MAPPING_ERROR
    }

    private fun checkSuccess(): Boolean {
        return currentApplicability == CandidateApplicability.RESOLVED
    }


    override fun consumeCandidate(group: Int, symbol: ConeSymbol, resolver: CallResolver) {
        if (symbol is ConeVariableSymbol && symbol.callableId.callableName == variableName) {
            variableChecker.consumeCandidate(group, symbol, resolver)

            val lastCandidate = variableChecker.candidates.lastOrNull()
            if (variableChecker.currentApplicability == CandidateApplicability.RESOLVED && lastCandidate == symbol) {

                val receiverScope =
                    resolver.typeCalculator.tryCalculateReturnType(lastCandidate.firUnsafe()).type
                        .scope(resolver.session)


                lookupInvoke = true

                receiverScope?.processFunctionsByName(invoke) { candidate ->
                    this.consumeCandidate(-1, candidate, resolver)
                    ProcessorAction.NEXT
                }
                if (checkSuccess()) return

                for ((index, scope) in resolver.processedScopes.withIndex()) {
                    scope.processFunctionsByName(invoke) { candidate ->
                        this.consumeCandidate(index, candidate, resolver)
                        ProcessorAction.NEXT
                    }
                    if (checkSuccess()) return
                }

            }
        }

        super.consumeCandidate(group, symbol, resolver)
    }


    companion object {
        val invoke = Name.identifier("invoke")
    }
}


class ClassifierApplicabilityChecker(val name: Name) : ApplicabilityChecker() {
    override fun updateNames(names: LinkedHashSet<Name>) {
        names.add(name)
    }

    override fun consumeCandidate(group: Int, symbol: ConeSymbol, resolver: CallResolver) {
        if (symbol !is ConeClassifierSymbol) return
        if (symbol.toLookupTag().name != name) return
        super.consumeCandidate(group, symbol, resolver)
    }

}

open class FunctionApplicabilityChecker(val name: Name) : ApplicabilityChecker() {

    var parameterCount = 0

    override fun updateNames(names: LinkedHashSet<Name>) {
        names.add(name)
    }

    lateinit var session: FirSession

    override fun getApplicability(
        group: Int,
        symbol: ConeSymbol,
        resolver: CallResolver
    ): CandidateApplicability {
        val declaration = symbol.firUnsafe<FirCallableDeclaration>()

        if (declaration is FirFunction && declaration.valueParameters.size != parameterCount) return CandidateApplicability.PARAMETER_MAPPING_ERROR


        var extensionReceiver = declaration.receiverTypeRef?.coneTypeUnsafe()
        var dispatchReceiver = declaration.dispatchReceiverType(session)

        val explicitReceiverType = explicitReceiverType?.coneTypeUnsafe()


        if (explicitReceiverType != null) {
            if (dispatchReceiver != null && explicitReceiverType.isSubtypeOf(dispatchReceiver, session)) {
                dispatchReceiver = null
            }
            if (extensionReceiver != null && explicitReceiverType.isSubtypeOf(extensionReceiver, session)) {
                extensionReceiver = null
            }
            if (extensionReceiver != null || dispatchReceiver != null) return CandidateApplicability.WRONG_RECEIVER
        }

        return CandidateApplicability.RESOLVED
    }

    fun ConeKotlinType.isSubtypeOf(type: ConeKotlinType, session: FirSession): Boolean {
        return AbstractTypeChecker.isSubtypeOf(ConeTypeCheckerContext(true, session), this, type)
    }

    override fun consumeCandidate(group: Int, symbol: ConeSymbol, resolver: CallResolver) {
        if (symbol !is ConeFunctionSymbol) return
        if (symbol.callableId.callableName != name) return
        session = resolver.session
        super.consumeCandidate(group, symbol, resolver)
    }
}

fun FirCallableDeclaration.dispatchReceiverType(session: FirSession): ConeKotlinType? {
    val id = (this.symbol as ConeCallableSymbol).callableId.classId ?: return null
    val symbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(id) as? FirClassSymbol ?: return null
    return symbol.fir.defaultType()
}