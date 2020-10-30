/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import com.intellij.lang.jvm.types.JvmPrimitiveTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirJavaSyntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.calls.syntheticNamesProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getContainingCallableNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.getContainingClassifierNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.impl.JavaPrimitiveTypeImpl
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
        getterSymbol: FirNamedFunctionSymbol,
        setterSymbol: FirNamedFunctionSymbol?,
        syntheticPropertyName: Name,
    ): FirAccessorSymbol? {
        return buildSyntheticProperty {
            session = this@JavaClassUseSiteMemberScope.session
            name = syntheticPropertyName
            symbol = FirAccessorSymbol(
                accessorId = getterSymbol.callableId,
                callableId = CallableId(getterSymbol.callableId.packageName, getterSymbol.callableId.className, syntheticPropertyName)
            )
            delegateGetter = getterSymbol.fir
            delegateSetter = setterSymbol?.fir
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
                var getterSymbol: FirNamedFunctionSymbol? = null
                var setterSymbol: FirNamedFunctionSymbol? = null
                declaredMemberScope.processFunctionsByName(getterName) { functionSymbol ->
                    if (getterSymbol == null && functionSymbol is FirNamedFunctionSymbol) {
                        val function = functionSymbol.fir
                        if (!function.isStatic && function.valueParameters.isEmpty()) {
                            getterSymbol = functionSymbol
                        }
                    }
                }
                val setterName = session.syntheticNamesProvider.setterNameByGetterName(getterName)
                if (getterSymbol != null && setterName != null) {
                    declaredMemberScope.processFunctionsByName(setterName) { functionSymbol ->
                        if (setterSymbol == null && functionSymbol is FirNamedFunctionSymbol) {
                            val function = functionSymbol.fir
                            if (!function.isStatic && function.valueParameters.size == 1) {
                                val returnTypeRef = function.returnTypeRef
                                if (returnTypeRef.isUnit) {
                                    setterSymbol = functionSymbol
                                } else if (returnTypeRef is FirJavaTypeRef) {
                                    val primitiveType = returnTypeRef.type as? JavaPrimitiveTypeImpl
                                    if (primitiveType?.psi?.kind == JvmPrimitiveTypeKind.VOID) {
                                        setterSymbol = functionSymbol
                                    }
                                }
                            }
                        }
                    }
                    val accessorSymbol = generateAccessorSymbol(
                        getterSymbol!!, setterSymbol, propertyName
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

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        // Do not generate accessors at all?
        if (name.isSpecial) {
            return processAccessorFunctionsAndPropertiesByName(name, emptyList(), processor)
        }
        val getterNames = FirJavaSyntheticNamesProvider.possibleGetterNamesByPropertyName(name)
        return processAccessorFunctionsAndPropertiesByName(name, getterNames, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        if (symbol.fir !is FirJavaClass) {
            return super.processFunctionsByName(name, processor)
        }
        val potentialPropertyName = session.syntheticNamesProvider.propertyNameByAccessorName(name)
            ?: return super.processFunctionsByName(name, processor)
        val accessors = mutableListOf<FirAccessorSymbol>()
        val getterName = session.syntheticNamesProvider.getterNameBySetterName(name) ?: name
        processAccessorFunctionsAndPropertiesByName(potentialPropertyName, listOf(getterName)) {
            if (it is FirAccessorSymbol) {
                accessors += it
            }
        }
        if (accessors.isEmpty()) {
            return super.processFunctionsByName(name, processor)
        }
        super.processFunctionsByName(name) { functionSymbol ->
            if (accessors.none { accessorSymbol ->
                    val syntheticProperty = accessorSymbol.fir as FirSyntheticProperty
                    syntheticProperty.getter.delegate === functionSymbol.fir ||
                            syntheticProperty.setter?.delegate === functionSymbol.fir
                }
            ) {
                processor(functionSymbol)
            }
        }
    }
}
