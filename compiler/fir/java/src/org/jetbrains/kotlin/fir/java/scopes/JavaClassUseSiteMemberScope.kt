/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.name.Name

class JavaClassUseSiteMemberScope(
    klass: FirRegularClass,
    session: FirSession,
    superTypesScope: FirScope,
    declaredMemberScope: FirScope
) : AbstractFirUseSiteMemberScope(
    session,
    JavaOverrideChecker(session, if (klass is FirJavaClass) klass.javaTypeParameterStack else JavaTypeParameterStack.EMPTY),
    superTypesScope,
    declaredMemberScope
) {
    internal val symbol = klass.symbol

    internal fun bindOverrides(name: Name) {
        val overrideCandidates = mutableSetOf<FirFunctionSymbol<*>>()
        declaredMemberScope.processFunctionsByName(name) {
            overrideCandidates += it
        }
        superTypesScope.processFunctionsByName(name) {
            it.getOverridden(overrideCandidates)
        }
    }

    private fun generateAccessorSymbol(
        functionSymbol: FirFunctionSymbol<*>,
        syntheticPropertyName: Name,
        overrideCandidates: MutableSet<FirCallableSymbol<*>>,
        isGetter: Boolean
    ): FirAccessorSymbol? {
        if (functionSymbol is FirNamedFunctionSymbol) {
            val fir = functionSymbol.fir
            if (fir.isStatic) {
                return null
            }
            when (isGetter) {
                true -> if (fir.valueParameters.isNotEmpty()) {
                    return null
                }
                false -> if (fir.valueParameters.size != 1) {
                    return null
                }
            }
        }
        overrideCandidates += functionSymbol
        val accessorSymbol = FirAccessorSymbol(
            accessorId = functionSymbol.callableId,
            callableId = CallableId(functionSymbol.callableId.packageName, functionSymbol.callableId.className, syntheticPropertyName)
        )
        if (functionSymbol is FirNamedFunctionSymbol) {
            functionSymbol.fir.let { callableMember -> accessorSymbol.bind(callableMember) }
        }
        return accessorSymbol
    }

    private fun processAccessorFunctionsAndPropertiesByName(
        propertyName: Name,
        getterNames: List<Name>,
        setterName: Name?,
        processor: (FirCallableSymbol<*>) -> Unit
    ): Unit {
        val overrideCandidates = mutableSetOf<FirCallableSymbol<*>>()
        val klass = symbol.fir
        declaredMemberScope.processPropertiesByName(propertyName) { variableSymbol ->
            overrideCandidates += variableSymbol
            processor(variableSymbol)
        }

        if (klass is FirJavaClass) {
            for (getterName in getterNames) {
                declaredMemberScope.processFunctionsByName(getterName) { functionSymbol ->
                    val accessorSymbol = generateAccessorSymbol(
                        functionSymbol, propertyName, overrideCandidates, isGetter = true
                    )
                    if (accessorSymbol != null) {
                        // NB: accessor should not be processed directly unless we find matching property symbol in supertype
                        overrideCandidates += accessorSymbol
                    }
                }
            }
        }

        superTypesScope.processPropertiesByName(propertyName) {
            val firCallableMember = it.fir as? FirCallableMemberDeclaration<*>
            if (firCallableMember?.isStatic == true) {
                processor(it)
            } else {
                when (val overriddenBy = it.getOverridden(overrideCandidates)) {
                    null -> processor(it)
                    is FirAccessorSymbol -> processor(overriddenBy)
                }
            }
        }
    }

    override fun createFunctionCopy(firSimpleFunction: FirSimpleFunction, newSymbol: FirNamedFunctionSymbol): FirSimpleFunctionImpl {
        if (firSimpleFunction !is FirJavaMethod) return super.createFunctionCopy(firSimpleFunction, newSymbol)
        return FirJavaMethod(
            firSimpleFunction.session,
            firSimpleFunction.source,
            newSymbol,
            firSimpleFunction.name,
            firSimpleFunction.visibility,
            firSimpleFunction.modality,
            firSimpleFunction.returnTypeRef as FirJavaTypeRef,
            firSimpleFunction.status.isStatic
        )
    }

    override fun createValueParameterCopy(parameter: FirValueParameter, newDefaultValue: FirExpression?): FirValueParameterImpl {
        if (parameter !is FirJavaValueParameter) return super.createValueParameterCopy(parameter, newDefaultValue)
        return FirJavaValueParameter(
            parameter.session,
            parameter.source,
            parameter.name,
            parameter.returnTypeRef as FirJavaTypeRef,
            parameter.isVararg
        )
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> Unit) {
        // Do not generate accessors at all?
        if (name.isSpecial) {
            return processAccessorFunctionsAndPropertiesByName(name, emptyList(), null, processor)
        }
        val getterNames = FirSyntheticPropertiesScope.possibleGetterNamesByPropertyName(name)
        val setterName = Name.identifier(SETTER_PREFIX + name.identifier.capitalize())
        return processAccessorFunctionsAndPropertiesByName(name, getterNames, setterName, processor)
    }

    companion object {
        private const val SETTER_PREFIX = "set"
    }
}
