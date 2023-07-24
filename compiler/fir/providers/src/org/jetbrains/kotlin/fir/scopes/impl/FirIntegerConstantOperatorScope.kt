/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.ConvertibleIntegerOperators.binaryOperatorsWithSignedArgument
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class FirIntegerConstantOperatorScope(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val isUnsigned: Boolean
) : FirTypeScope() {
    private val baseScope: FirTypeScope = run {
        val baseType = when (isUnsigned) {
            true -> session.builtinTypes.uIntType
            false -> session.builtinTypes.intType
        }.type

        baseType.scope(
            session,
            scopeSession,
            FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        ) ?: Empty
    }

    private val mappedFunctions = mutableMapOf<Name, FirNamedFunctionSymbol>()

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        // Constant conversion for those unary operators works only for signed integers
        val isUnaryOperator = !isUnsigned && (name in ConvertibleIntegerOperators.unaryOperatorNames)
        val isBinaryOperator = name in ConvertibleIntegerOperators.binaryOperatorsNames
        if (!isUnaryOperator && !isBinaryOperator) {
            return baseScope.processFunctionsByName(name, processor)
        }
        val requiresUnsignedOperand = isUnsigned && name !in binaryOperatorsWithSignedArgument
        val wrappedSymbol = mappedFunctions.getOrPut(name) {
            val allFunctions = baseScope.getFunctions(name)
            val functionSymbol = allFunctions.first {
                // unary operators have only one overload
                if (isUnaryOperator) return@first true

                val coneType = it.fir.valueParameters.first().returnTypeRef.coneType
                if (requiresUnsignedOperand) {
                    coneType.isUInt
                } else {
                    coneType.isInt
                }
            }
            wrapIntOperator(functionSymbol)
        }
        processor(wrappedSymbol)
        baseScope.processFunctionsByName(name, processor)
    }

    private fun wrapIntOperator(originalSymbol: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        val originalFunction = originalSymbol.fir
        val wrappedFunction = buildSimpleFunctionCopy(originalFunction) {
            symbol = FirNamedFunctionSymbol(originalSymbol.callableId)
            origin = FirDeclarationOrigin.WrappedIntegerOperator
            returnTypeRef = buildResolvedTypeRef {
                type = ConeIntegerConstantOperatorTypeImpl(isUnsigned, ConeNullability.NOT_NULL)
            }
        }.also {
            it.originalForWrappedIntegerOperator = originalSymbol
            it.isUnsignedWrappedIntegerOperator = isUnsigned
        }
        return wrappedFunction.symbol
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        baseScope.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        baseScope.processDeclaredConstructors(processor)
    }

    override fun mayContainName(name: Name): Boolean {
        return baseScope.mayContainName(name)
    }

    override fun getCallableNames(): Set<Name> {
        return baseScope.getCallableNames()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        // Int types don't have nested classifiers
    }

    override fun getClassifierNames(): Set<Name> {
        return emptySet()
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return ProcessorAction.NONE
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return ProcessorAction.NONE
    }
}

fun ScopeSession.getOrBuildScopeForIntegerConstantOperatorType(
    session: FirSession,
    type: ConeIntegerConstantOperatorType
): FirIntegerConstantOperatorScope {
    return getOrBuild(type.isUnsigned, INTEGER_CONSTANT_OPERATOR_SCOPE) {
        FirIntegerConstantOperatorScope(session, this, type.isUnsigned)
    }
}

private val INTEGER_CONSTANT_OPERATOR_SCOPE = scopeSessionKey<Boolean, FirIntegerConstantOperatorScope>()

private object OriginalForWrappedIntegerOperator : FirDeclarationDataKey()
private object IsUnsignedForWrappedIntegerOperator : FirDeclarationDataKey()

var FirSimpleFunction.originalForWrappedIntegerOperator: FirNamedFunctionSymbol? by FirDeclarationDataRegistry.data(
    OriginalForWrappedIntegerOperator
)

private var FirSimpleFunction.isUnsignedWrappedIntegerOperator: Boolean? by FirDeclarationDataRegistry.data(
    IsUnsignedForWrappedIntegerOperator
)

@OptIn(ExperimentalContracts::class)
fun FirDeclaration.isWrappedIntegerOperator(): Boolean {
    contract {
        returns(true) implies (this@isWrappedIntegerOperator is FirSimpleFunction)
    }
    return (this as? FirSimpleFunction)?.originalForWrappedIntegerOperator != null
}

@OptIn(ExperimentalContracts::class)
fun FirBasedSymbol<*>.isWrappedIntegerOperator(): Boolean {
    contract {
        returns(true) implies (this@isWrappedIntegerOperator is FirNamedFunctionSymbol)
    }
    return fir.isWrappedIntegerOperator()
}

@OptIn(ExperimentalContracts::class)
fun FirBasedSymbol<*>.isWrappedIntegerOperatorForUnsignedType(): Boolean {
    return (this as? FirNamedFunctionSymbol)?.fir?.isUnsignedWrappedIntegerOperator ?: false
}
