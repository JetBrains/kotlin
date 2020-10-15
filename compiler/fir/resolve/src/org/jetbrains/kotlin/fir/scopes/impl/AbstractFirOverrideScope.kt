/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirPropertyBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirValueParameterBuilder
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

abstract class AbstractFirOverrideScope(
    val session: FirSession,
    protected val overrideChecker: FirOverrideChecker
) : FirTypeScope() {
    //base symbol as key, overridden as value
    val overrideByBase = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>?>()

    private fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        return overrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration)
    }

    private fun isOverriddenProperty(overrideCandidate: FirCallableMemberDeclaration<*>, baseDeclaration: FirProperty): Boolean {
        return overrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)
    }

    protected fun similarFunctionsOrBothProperties(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableMemberDeclaration<*>
    ): Boolean {
        return when (overrideCandidate) {
            is FirSimpleFunction -> when (baseDeclaration) {
                is FirSimpleFunction -> isOverriddenFunction(overrideCandidate, baseDeclaration)
                is FirProperty -> isOverriddenProperty(overrideCandidate, baseDeclaration)
                else -> false
            }
            is FirConstructor -> false
            is FirProperty -> baseDeclaration is FirProperty && isOverriddenProperty(overrideCandidate, baseDeclaration)
            is FirField -> baseDeclaration is FirField
            else -> error("Unknown fir callable type: $overrideCandidate, $baseDeclaration")
        }
    }

    // Receiver is super-type function here
    protected open fun FirCallableSymbol<*>.getOverridden(overrideCandidates: Set<FirCallableSymbol<*>>): FirCallableSymbol<*>? {
        if (overrideByBase.containsKey(this)) return overrideByBase[this]

        val baseDeclaration = (this as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration<*>
        val override = overrideCandidates.firstOrNull {
            val overrideCandidate = (it as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration<*>
            baseDeclaration.modality != Modality.FINAL && similarFunctionsOrBothProperties(overrideCandidate, baseDeclaration)
        } // TODO: two or more overrides for one fun?
        overrideByBase[this] = override
        return override
    }

    protected open fun createFunctionCopy(
        firSimpleFunction: FirSimpleFunction,
        newSymbol: FirNamedFunctionSymbol
    ): FirSimpleFunctionBuilder =
        FirSimpleFunctionBuilder().apply {
            source = firSimpleFunction.source
            session = firSimpleFunction.session
            resolvePhase = firSimpleFunction.resolvePhase
            origin = FirDeclarationOrigin.SubstitutionOverride
            returnTypeRef = firSimpleFunction.returnTypeRef
            receiverTypeRef = firSimpleFunction.receiverTypeRef
            name = firSimpleFunction.name
            status = firSimpleFunction.status
            symbol = newSymbol
        }

    protected open fun createValueParameterCopy(parameter: FirValueParameter, newDefaultValue: FirExpression?): FirValueParameterBuilder =
        FirValueParameterBuilder().apply {
            source = parameter.source
            session = parameter.session
            resolvePhase = parameter.resolvePhase
            origin = FirDeclarationOrigin.SubstitutionOverride
            returnTypeRef = parameter.returnTypeRef
            name = parameter.name
            symbol = FirVariableSymbol(parameter.symbol.callableId)
            defaultValue = newDefaultValue
            isCrossinline = parameter.isCrossinline
            isNoinline = parameter.isNoinline
            isVararg = parameter.isVararg
        }

    protected open fun createPropertyCopy(
        firProperty: FirProperty,
        newSymbol: FirPropertySymbol
    ): FirPropertyBuilder =
        FirPropertyBuilder().apply {
            source = firProperty.source
            session = firProperty.session
            resolvePhase = firProperty.resolvePhase
            origin = FirDeclarationOrigin.SubstitutionOverride
            returnTypeRef = firProperty.returnTypeRef
            receiverTypeRef = firProperty.receiverTypeRef
            isVar = firProperty.isVar
            isLocal = firProperty.isLocal
            getter = firProperty.getter
            setter = firProperty.setter
            name = firProperty.name
            status = firProperty.status
            symbol = newSymbol
        }

}
