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
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord

class JavaClassUseSiteMemberScope(
    klass: FirRegularClass,
    session: FirSession,
    superTypesScope: FirSuperTypeScope,
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
            NEXT
        }


        superTypesScope.processFunctionsByName(name) {
            it.getOverridden(overrideCandidates)
            NEXT
        }
    }

    private fun processAccessorFunction(
        functionSymbol: FirFunctionSymbol<*>,
        syntheticPropertyName: Name,
        overrideCandidates: MutableSet<FirCallableSymbol<*>>,
        processor: (FirCallableSymbol<*>) -> ProcessorAction,
        isGetter: Boolean
    ): ProcessorAction {
        if (functionSymbol is FirNamedFunctionSymbol) {
            val fir = functionSymbol.fir
            if (fir.isStatic) {
                return NEXT
            }
            when (isGetter) {
                true -> if (fir.valueParameters.isNotEmpty()) {
                    return NEXT
                }
                false -> if (fir.valueParameters.size != 1) {
                    return NEXT
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
        return processor(accessorSymbol)
    }

    private fun processAccessorFunctionsAndPropertiesByName(
        propertyName: Name,
        getterNames: List<Name>,
        setterName: Name,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        val overrideCandidates = mutableSetOf<FirCallableSymbol<*>>()
        val klass = symbol.fir
        if (!declaredMemberScope.processPropertiesByName(propertyName) { variableSymbol ->
                overrideCandidates += variableSymbol
                processor(variableSymbol)
            }
        ) return STOP
        if (klass is FirJavaClass) {
            for (getterName in getterNames) {
                if (!declaredMemberScope.processFunctionsByName(getterName) { functionSymbol ->
                        processAccessorFunction(functionSymbol, propertyName, overrideCandidates, processor, isGetter = true)
                    }
                ) return STOP
            }
        }

        return superTypesScope.processPropertiesByName(propertyName) {
            val firCallableMember = it.fir as? FirCallableMemberDeclaration<*>
            if (firCallableMember?.isStatic == true) {
                processor(it)
            } else {
                val overriddenBy = it.getOverridden(overrideCandidates)
                if (overriddenBy == null) {
                    processor(it)
                } else {
                    NEXT
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

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val identifier = name.identifier
        val capitalizedAsciiName = identifier.capitalizeAsciiOnly()
        val capitalizedFirstWordName = identifier.capitalizeFirstWord(asciiOnly = true)
        val getterNames = listOfNotNull(
            Name.identifier(GETTER_PREFIX + capitalizedAsciiName),
            if (capitalizedFirstWordName == capitalizedAsciiName) null else Name.identifier(GETTER_PREFIX + capitalizedFirstWordName),
            name.takeIf { identifier.startsWith(IS_PREFIX) }
        ).filter {
            propertyNameByGetMethodName(it) == name
        }
        val setterName = Name.identifier(SETTER_PREFIX + name.asString().capitalize())
        return processAccessorFunctionsAndPropertiesByName(name, getterNames, setterName, processor)
    }

    companion object {
        private const val GETTER_PREFIX = "get"

        private const val SETTER_PREFIX = "set"

        private const val IS_PREFIX = "is"
    }
}
