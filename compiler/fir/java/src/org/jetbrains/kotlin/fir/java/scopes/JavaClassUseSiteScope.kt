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
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractProviderBasedScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.Name

class JavaClassUseSiteScope(
    klass: FirRegularClass,
    session: FirSession,
    internal val superTypesScope: FirScope,
    private val declaredMemberScope: FirScope
) : FirAbstractProviderBasedScope(session, lookupInFir = true) {
    internal val symbol = klass.symbol

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (klass is FirJavaClass) klass.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    //base symbol as key, overridden as value
    internal val overriddenByBase = mutableMapOf<ConeCallableSymbol, ConeFunctionSymbol?>()

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

    private fun isOverriddenFunCheck(overriddenInJava: FirFunction, base: FirFunction): Boolean {
        overriddenInJava as FirCallableMemberDeclaration
        val receiverTypeRef = (base as FirCallableMemberDeclaration).receiverTypeRef
        val baseParameterTypes = listOfNotNull(receiverTypeRef) + base.valueParameters.map { it.returnTypeRef }

        if (overriddenInJava.valueParameters.size != baseParameterTypes.size) return false
        if (overriddenInJava.typeParameters.size != base.typeParameters.size) return false

        val types = base.typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol, false)
        }
        val substitution = ConeSubstitutorByMap(overriddenInJava.typeParameters.map { it.symbol }.zip(types).toMap())
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

    internal fun bindOverrides(name: Name) {
        val overrideCandidates = mutableSetOf<ConeFunctionSymbol>()
        declaredMemberScope.processFunctionsByName(name) {
            overrideCandidates += it
            ProcessorAction.NEXT
        }


        superTypesScope.processFunctionsByName(name) {
            it.getOverridden(overrideCandidates)
            ProcessorAction.NEXT
        }
    }

    private fun ConeCallableSymbol.getOverridden(candidates: Set<ConeFunctionSymbol>): ConeCallableSymbol? {
        if (overriddenByBase.containsKey(this)) return overriddenByBase[this]

        val overriding = when (this) {
            is FirFunctionSymbol -> {
                val self = firUnsafe<FirFunction>()
                self as FirCallableMemberDeclaration
                candidates.firstOrNull {
                    val member = (it as FirFunctionSymbol).fir as FirFunction
                    self.modality != Modality.FINAL && isOverriddenFunCheck(member, self)
                }
            }
            is FirPropertySymbol -> {
                val self = fir as? FirProperty ?: return null
                candidates.firstOrNull {
                    val member = (it as FirFunctionSymbol).fir as FirNamedFunction
                    self.modality != Modality.FINAL && isOverriddenPropertyCheck(member, self)

                }
            }
            is FirAccessorSymbol -> {
                val self = fir as FirNamedFunction
                candidates.firstOrNull {
                    val member = (it as FirFunctionSymbol).fir as FirNamedFunction
                    self.modality != Modality.FINAL && isOverriddenFunCheck(member, self)
                }
            }
            else -> error("Unexpected callable symbol: $this")
        }
        // TODO: two or more overrides for one fun?
        overriddenByBase[this] = overriding
        return overriding
    }

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val overrideCandidates = mutableSetOf<ConeFunctionSymbol>()
        if (!declaredMemberScope.processFunctionsByName(name) {
                overrideCandidates += it
                processor(it)
            }
        ) return ProcessorAction.STOP

        return superTypesScope.processFunctionsByName(name) {

            val overriddenBy = it.getOverridden(overrideCandidates)
            if (overriddenBy == null) {
                processor(it)
            } else {
                ProcessorAction.NEXT
            }
        }
    }

    private fun processAccessorFunctionsByName(
        propertyName: Name,
        accessorName: Name,
        processor: (ConePropertySymbol) -> ProcessorAction
    ): ProcessorAction {
        val overrideCandidates = mutableSetOf<ConeFunctionSymbol>()
        if (!declaredMemberScope.processFunctionsByName(accessorName) { functionSymbol ->
                overrideCandidates += functionSymbol
                val accessorSymbol = FirAccessorSymbol(
                    accessorId = functionSymbol.callableId,
                    callableId = CallableId(functionSymbol.callableId.packageName, functionSymbol.callableId.className, propertyName)
                )
                if (functionSymbol is FirBasedSymbol<*>) {
                    (functionSymbol.fir as? FirCallableMemberDeclaration)?.let { callableMember -> accessorSymbol.bind(callableMember) }
                }
                processor(accessorSymbol)
            }
        ) return ProcessorAction.STOP

        return superTypesScope.processPropertiesByName(propertyName) {
            val firCallableMember = (it as FirBasedSymbol<*>).fir as? FirCallableMemberDeclaration
            if (firCallableMember?.isStatic == true) {
                ProcessorAction.NEXT
            } else {
                val overriddenBy = it.getOverridden(overrideCandidates)
                if (overriddenBy == null && it is ConePropertySymbol) {
                    processor(it)
                } else {
                    ProcessorAction.NEXT
                }
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        if (!declaredMemberScope.processPropertiesByName(name) {
                processor(it)
            }
        ) return ProcessorAction.STOP

        val getterName = Name.identifier(getterPrefix + name.asString().capitalize())
        return processAccessorFunctionsByName(name, getterName, processor)
    }

    companion object {
        private const val getterPrefix = "get"
    }
}
