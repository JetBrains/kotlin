/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

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

    private fun processAccessorFunctionsAndPropertiesByName(
        propertyName: Name,
        accessorName: Name,
        isGetter: Boolean,
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
            if (!declaredMemberScope.processFunctionsByName(accessorName) { functionSymbol ->
                    if (functionSymbol is FirNamedFunctionSymbol) {
                        val fir = functionSymbol.fir
                        if (fir.isStatic) {
                            return@processFunctionsByName NEXT
                        }
                        when (isGetter) {
                            true -> if (fir.valueParameters.isNotEmpty()) {
                                return@processFunctionsByName NEXT
                            }
                            false -> if (fir.valueParameters.size != 1) {
                                return@processFunctionsByName NEXT
                            }
                        }
                    }
                    overrideCandidates += functionSymbol
                    val accessorSymbol = FirAccessorSymbol(
                        accessorId = functionSymbol.callableId,
                        callableId = CallableId(functionSymbol.callableId.packageName, functionSymbol.callableId.className, propertyName)
                    )
                    if (functionSymbol is FirNamedFunctionSymbol) {
                        functionSymbol.fir.let { callableMember -> accessorSymbol.bind(callableMember) }
                    }
                    processor(accessorSymbol)
                }
            ) return STOP
        }

        return superTypesScope.processPropertiesByName(propertyName) {
            val firCallableMember = it.fir as? FirCallableMemberDeclaration<*>
            if (firCallableMember?.isStatic == true) {
                processor(it)
            } else {
                val overriddenBy = it.getOverridden(overrideCandidates)
                if (overriddenBy == null && it is FirVariableSymbol<*>) {
                    processor(it)
                } else {
                    NEXT
                }
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val getterName = Name.identifier(getterPrefix + name.asString().capitalize())
        return processAccessorFunctionsAndPropertiesByName(name, getterName, isGetter = true, processor = processor)
    }

    companion object {
        private const val getterPrefix = "get"
    }
}
