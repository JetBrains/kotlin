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
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.*
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.Name

class JavaClassUseSiteMemberScope(
    klass: FirRegularClass,
    session: FirSession,
    superTypesScope: FirSuperTypeScope,
    declaredMemberScope: FirScope
) : AbstractFirUseSiteMemberScope(session, superTypesScope, declaredMemberScope) {
    internal val symbol = klass.symbol

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (klass is FirJavaClass) klass.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    private fun isEqualTypes(a: ConeKotlinType, b: ConeKotlinType, substitutor: ConeSubstitutor): Boolean {
        if (a is ConeFlexibleType) return isEqualTypes(a.lowerBound, b, substitutor)
        if (b is ConeFlexibleType) return isEqualTypes(a, b.lowerBound, substitutor)
        return if (a is ConeClassType && b is ConeClassType) {
            a.lookupTag.classId.let { it.readOnlyToMutable() ?: it } == b.lookupTag.classId.let { it.readOnlyToMutable() ?: it }
        } else {
            with(context) {
                isEqualTypeConstructors(
                    substitutor.substituteOrSelf(a).typeConstructor(),
                    substitutor.substituteOrSelf(b).typeConstructor()
                )
            }
        }
    }

    private fun isEqualTypes(a: FirTypeRef, b: FirTypeRef, substitutor: ConeSubstitutor) =
        isEqualTypes(
            a.toNotNullConeKotlinType(session, javaTypeParameterStack),
            b.toNotNullConeKotlinType(session, javaTypeParameterStack),
            substitutor
        )

    override fun isOverriddenFunCheck(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        // NB: overrideCandidate is from Java and has no receiver
        val receiverTypeRef = baseDeclaration.receiverTypeRef
        val baseParameterTypes = listOfNotNull(receiverTypeRef) + baseDeclaration.valueParameters.map { it.returnTypeRef }

        if (overrideCandidate.valueParameters.size != baseParameterTypes.size) return false
        if (overrideCandidate.typeParameters.size != baseDeclaration.typeParameters.size) return false

        val types = baseDeclaration.typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
        }
        val substitution = substitutorByMap(overrideCandidate.typeParameters.map { it.symbol }.zip(types).toMap())
        if (!overrideCandidate.typeParameters.zip(baseDeclaration.typeParameters).all { (a, b) ->
                a.bounds.size == b.bounds.size && a.bounds.zip(b.bounds).all { (aBound, bBound) ->
                    isEqualTypes(aBound, bBound, substitution)
                }
            }
        ) return false


        return overrideCandidate.valueParameters.zip(baseParameterTypes).all { (paramFromJava, baseType) ->
            isEqualTypes(paramFromJava.returnTypeRef, baseType, substitution)
        }
    }

    override fun isOverriddenPropertyCheck(overrideCandidate: FirCallableMemberDeclaration<*>, baseDeclaration: FirProperty): Boolean {
        if (baseDeclaration.modality == Modality.FINAL) return false
        val receiverTypeRef = baseDeclaration.receiverTypeRef
        return when (overrideCandidate) {
            is FirSimpleFunction -> {
                if (receiverTypeRef == null) {
                    // TODO: setters
                    return overrideCandidate.valueParameters.isEmpty()
                } else {
                    if (overrideCandidate.valueParameters.size != 1) return false
                    return isEqualTypes(receiverTypeRef, overrideCandidate.valueParameters.single().returnTypeRef, ConeSubstitutor.Empty)
                }
            }
            is FirProperty -> {
                val overrideReceiverTypeRef = overrideCandidate.receiverTypeRef
                return when {
                    receiverTypeRef == null -> overrideReceiverTypeRef == null
                    overrideReceiverTypeRef == null -> false
                    else -> isEqualTypes(receiverTypeRef, overrideReceiverTypeRef, ConeSubstitutor.Empty)
                }
            }
            else -> false
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
