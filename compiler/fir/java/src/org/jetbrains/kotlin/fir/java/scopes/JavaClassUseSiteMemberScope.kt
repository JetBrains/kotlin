/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirValueParameterBuilder
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirJavaSyntheticNamesProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getContainingCallableNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.getContainingClassifierNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.name.Name

class JavaClassUseSiteMemberScope(
    klass: FirRegularClass,
    session: FirSession,
    superTypesScope: FirTypeScope,
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

    override fun getCallableNames(): Set<Name> {
        return declaredMemberScope.getContainingCallableNamesIfPresent() + superTypesScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getContainingClassifierNamesIfPresent() + superTypesScope.getClassifierNames()
    }

    private fun generateAccessorSymbol(
        functionSymbol: FirFunctionSymbol<*>,
        syntheticPropertyName: Name,
        overrideCandidates: MutableSet<FirCallableSymbol<*>>,
        isGetter: Boolean
    ): FirAccessorSymbol? {
        if (functionSymbol !is FirNamedFunctionSymbol) {
            return null
        }
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
        overrideCandidates += functionSymbol
        return buildSyntheticProperty {
            session = this@JavaClassUseSiteMemberScope.session
            name = syntheticPropertyName
            symbol = FirAccessorSymbol(
                accessorId = functionSymbol.callableId,
                callableId = CallableId(functionSymbol.callableId.packageName, functionSymbol.callableId.className, syntheticPropertyName)
            )
            delegateGetter = fir
        }.symbol
    }

    private fun processAccessorFunctionsAndPropertiesByName(
        propertyName: Name,
        getterNames: List<Name>,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        val overrideCandidates = mutableSetOf<FirCallableSymbol<*>>()
        val klass = symbol.fir
        declaredMemberScope.processPropertiesByName(propertyName) { variableSymbol ->
            if (variableSymbol.isStatic) return@processPropertiesByName
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
            when (val overriddenBy = it.getOverridden(overrideCandidates)) {
                null -> processor(it)
                is FirAccessorSymbol -> processor(overriddenBy)
                is FirPropertySymbol -> if (it is FirPropertySymbol) {
                    directOverriddenProperties.getOrPut(overriddenBy) { mutableListOf() }.add(it)
                }
            }
        }
    }

    override fun createFunctionCopy(firSimpleFunction: FirSimpleFunction, newSymbol: FirNamedFunctionSymbol): FirSimpleFunctionBuilder {
        if (firSimpleFunction !is FirJavaMethod) return super.createFunctionCopy(firSimpleFunction, newSymbol)
        return FirJavaMethodBuilder().apply {
            session = firSimpleFunction.session
            source = firSimpleFunction.source
            symbol = newSymbol
            name = firSimpleFunction.name
            visibility = firSimpleFunction.visibility
            modality = firSimpleFunction.modality
            returnTypeRef = firSimpleFunction.returnTypeRef
            isStatic = firSimpleFunction.status.isStatic
            status = firSimpleFunction.status
        }
    }

    override fun createValueParameterCopy(parameter: FirValueParameter, newDefaultValue: FirExpression?): FirValueParameterBuilder {
        if (parameter !is FirJavaValueParameter) return super.createValueParameterCopy(parameter, newDefaultValue)
        return FirJavaValueParameterBuilder().apply {
            session = parameter.session
            source = parameter.source
            name = parameter.name
            returnTypeRef = parameter.returnTypeRef as FirJavaTypeRef
            isVararg = parameter.isVararg
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        // Do not generate accessors at all?
        if (name.isSpecial) {
            return processAccessorFunctionsAndPropertiesByName(name, emptyList(), processor)
        }
        val getterNames = FirJavaSyntheticNamesProvider.possibleGetterNamesByPropertyName(name)
        val setterName = Name.identifier(SETTER_PREFIX + name.identifier.capitalize())
        return processAccessorFunctionsAndPropertiesByName(name, getterNames, processor)
    }

    companion object {
        private const val SETTER_PREFIX = "set"
    }
}
