/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.*
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractProviderBasedScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.Name

class JavaClassUseSiteScope(
    klass: FirRegularClass,
    session: FirSession,
    private val superTypesScope: FirSuperTypeScope,
    private val declaredMemberScope: FirScope
) : FirAbstractProviderBasedScope(session, lookupInFir = true) {
    internal val symbol = klass.symbol

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (klass is FirJavaClass) klass.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    //base symbol as key, overridden as value
    internal val overriddenByBase = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>?>()

    private val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(a: ConeKotlinType, b: ConeKotlinType, substitutor: ConeSubstitutor): Boolean {
        if (a is ConeFlexibleType) return isEqualTypes(a.lowerBound, b, substitutor)
        if (b is ConeFlexibleType) return isEqualTypes(a, b.lowerBound, substitutor)
        with(context) {
            return isEqualTypeConstructors(
                substitutor.substituteOrSelf(a).typeConstructor(),
                substitutor.substituteOrSelf(b).typeConstructor()
            )
        }
    }

    private fun isEqualTypes(a: FirTypeRef, b: FirTypeRef, substitutor: ConeSubstitutor) =
        isEqualTypes(
            a.toNotNullConeKotlinType(session, javaTypeParameterStack),
            b.toNotNullConeKotlinType(session, javaTypeParameterStack),
            substitutor
        )

    private fun isOverriddenFunCheck(overriddenInJava: FirNamedFunction, base: FirNamedFunction): Boolean {
        val receiverTypeRef = base.receiverTypeRef
        val baseParameterTypes = listOfNotNull(receiverTypeRef) + base.valueParameters.map { it.returnTypeRef }

        if (overriddenInJava.valueParameters.size != baseParameterTypes.size) return false
        if (overriddenInJava.typeParameters.size != base.typeParameters.size) return false

        val types = base.typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
        }
        val substitution = substitutorByMap(overriddenInJava.typeParameters.map { it.symbol }.zip(types).toMap())
        if (!overriddenInJava.typeParameters.zip(base.typeParameters).all { (a, b) ->
                a.bounds.size == b.bounds.size && a.bounds.zip(b.bounds).all { (aBound, bBound) ->
                    isEqualTypes(aBound, bBound, substitution)
                }
            }
        ) return false


        return overriddenInJava.valueParameters.zip(baseParameterTypes).all { (paramFromJava, baseType) ->
            isEqualTypes(paramFromJava.returnTypeRef, baseType, substitution)
        }
    }

    private fun isOverriddenPropertyCheck(overriddenInJava: FirNamedFunction, base: FirProperty): Boolean {
        val receiverTypeRef = base.receiverTypeRef
        if (receiverTypeRef == null) {
            // TODO: setters
            return overriddenInJava.valueParameters.isEmpty()
        } else {
            if (overriddenInJava.valueParameters.size != 1) return false
            return isEqualTypes(receiverTypeRef, overriddenInJava.valueParameters.single().returnTypeRef, ConeSubstitutor.Empty)
        }
    }

    private fun isOverriddenPropertyCheck(overriddenInKotlin: FirProperty, base: FirProperty): Boolean {
        val receiverTypeRef = base.receiverTypeRef
        val overriddenReceiverTypeRef = overriddenInKotlin.receiverTypeRef
        return when {
            receiverTypeRef == null -> overriddenReceiverTypeRef == null
            overriddenReceiverTypeRef == null -> false
            else -> isEqualTypes(receiverTypeRef, overriddenReceiverTypeRef, ConeSubstitutor.Empty)
        }
    }

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

    private fun FirCallableSymbol<*>.getOverridden(candidates: Set<FirCallableSymbol<*>>): FirCallableSymbol<*>? {
        if (overriddenByBase.containsKey(this)) return overriddenByBase[this]

        val overriding = when (this) {
            is FirNamedFunctionSymbol -> {
                val self = this.fir
                candidates.firstOrNull {
                    val overridden = (it as? FirNamedFunctionSymbol)?.fir
                    overridden != null && self.modality != Modality.FINAL && isOverriddenFunCheck(overridden, self)
                }
            }
            is FirConstructorSymbol, is FirFieldSymbol -> {
                null
            }
            is FirPropertySymbol -> {
                val self = fir
                candidates.firstOrNull {
                    when (it) {
                        is FirNamedFunctionSymbol -> {
                            val overridden = it.fir
                            self.modality != Modality.FINAL && isOverriddenPropertyCheck(overridden, self)
                        }
                        is FirPropertySymbol -> {
                            val overridden = it.fir
                            self.modality != Modality.FINAL && isOverriddenPropertyCheck(overridden, self)
                        }
                        else -> false
                    }

                }
            }
            is FirAccessorSymbol -> {
                val self = fir
                candidates.firstOrNull {
                    val overridden = (it as? FirNamedFunctionSymbol)?.fir
                    overridden != null && self.modality != Modality.FINAL && isOverriddenFunCheck(overridden, self)
                }
            }
            else -> error("Unexpected callable symbol: $this")
        }
        // TODO: two or more overrides for one fun?
        overriddenByBase[this] = overriding
        return overriding
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val overrideCandidates = mutableSetOf<FirFunctionSymbol<*>>()
        if (!declaredMemberScope.processFunctionsByName(name) {
                overrideCandidates += it
                processor(it)
            }
        ) return STOP

        return superTypesScope.processFunctionsByName(name) {

            val overriddenBy = it.getOverridden(overrideCandidates)
            if (overriddenBy == null) {
                processor(it)
            } else {
                NEXT
            }
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
                NEXT
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
