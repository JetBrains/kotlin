/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirGenericSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.substitutorForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType

internal class KaFirSubstitutorProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaSubstitutorProvider, KaFirSessionComponent {
    override fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? {
        withValidityAssertion {
            if (subClass == superClass) return KaSubstitutor.Empty(token)

            val baseFirSymbol = subClass.firSymbol
            val superFirSymbol = superClass.firSymbol
            val inheritancePath = collectInheritancePath(baseFirSymbol, superFirSymbol) ?: return null
            val substitutors = inheritancePath.map { (type, symbol) ->
                type.substitutorForSuperType(rootModuleSession, symbol)
            }
            return when (substitutors.size) {
                0 -> KaSubstitutor.Empty(token)
                else -> {
                    val chained = substitutors.reduce { left, right -> left.chain(right) }
                    firSymbolBuilder.typeBuilder.buildSubstitutor(chained)
                }
            }
        }
    }

    private fun collectInheritancePath(
        baseSymbol: FirClassSymbol<*>,
        superSymbol: FirClassSymbol<*>,
    ): List<Pair<ConeClassLikeType, FirRegularClassSymbol>>? {
        val stack = mutableListOf<Pair<ConeClassLikeType, FirRegularClassSymbol>>()
        var result: List<Pair<ConeClassLikeType, FirRegularClassSymbol>>? = null

        fun dfs(symbol: FirClassSymbol<*>) {
            for (superType in symbol.resolvedSuperTypes) {
                if (result != null) {
                    return
                }
                if (superType !is ConeClassLikeType) continue
                val superClassSymbol = superType.toRegularClassSymbol(rootModuleSession) ?: continue
                stack += superType to superClassSymbol
                if (superClassSymbol == superSymbol) {
                    result = stack.toList()
                    check(stack.removeLast().second == superClassSymbol)
                    break
                }
                dfs(superClassSymbol)
                check(stack.removeLast().second == superClassSymbol)
            }
        }

        dfs(baseSymbol)
        return result?.reversed()
    }

    override fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor = withValidityAssertion {
        if (mappings.isEmpty()) return KaSubstitutor.Empty(token)

        val firSubstitution = buildMap {
            mappings.forEach { (ktTypeParameterSymbol, ktType) ->
                check(ktTypeParameterSymbol is KaFirTypeParameterSymbol)
                check(ktType is KaFirType)
                put(ktTypeParameterSymbol.firSymbol, ktType.coneType)
            }
        }

        return when (val coneSubstitutor = substitutorByMap(firSubstitution, analysisSession.firSession)) {
            is ConeSubstitutorByMap -> KaFirMapBackedSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
            else -> KaFirGenericSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
        }
    }

    override fun createSubtypingSubstitutor(subClass: KaClassSymbol, superType: KaClassType): KaSubstitutor? = withValidityAssertion {
        with(analysisSession) {
            val superClassSymbol = superType.expandedSymbol ?: return null
            val expandedSuperType = superType.fullyExpandedType as? KaClassType ?: return null

            if (subClass == superClassSymbol) {
                return buildSubstitutorFromTypeArguments(subClass, expandedSuperType)
            }

            val inheritanceSubstitutor = createInheritanceTypeSubstitutor(subClass, superClassSymbol) ?: return null

            val superClassTypeParameters = superClassSymbol.typeParameters
            val typeArguments = expandedSuperType.typeArguments

            if (superClassTypeParameters.size != typeArguments.size) return null

            // Phase 1: Build mappings for direct type parameter associations
            val mappings = mutableMapOf<KaTypeParameterSymbol, KaType>()
            val concreteTypeCandidates = mutableListOf<Pair<KaType, KaType>>()

            for ((typeParameter, typeArgument) in superClassTypeParameters.zip(typeArguments)) {
                val concreteType = typeArgument.type ?: return null

                // Get the substituted type: what does this superclass type parameter map to in the subclass?
                val substitutedType = inheritanceSubstitutor.substitute(buildTypeParameterType(typeParameter))

                // If the substituted type is a type parameter type belonging to subclass, map it to the concrete type
                if (substitutedType is KaTypeParameterType) {
                    val existingMapping = mappings[substitutedType.symbol]
                    if (existingMapping != null) {
                        // Check for ambiguity: the same type parameter maps to different types
                        if (!existingMapping.semanticallyEquals(concreteType)) {
                            return null
                        }
                    } else {
                        mappings[substitutedType.symbol] = concreteType
                    }
                } else {
                    // The substituted type is a concrete (possibly generic) type - defer validation until Phase 2
                    // because we want to be able to substitute its type arguments with our complete substitutor
                    concreteTypeCandidates.add(substitutedType to concreteType)
                }
            }

            val substitutor = createSubstitutor(mappings)

            // Phase 2: Validate concrete types by applying the candidate substitutor
            if (concreteTypeCandidates.isNotEmpty()) {
                for ((substitutedType, expectedType) in concreteTypeCandidates) {
                    val fullySubstitutedType = substitutor.substitute(substitutedType)
                    if (!fullySubstitutedType.semanticallyEquals(expectedType)) {
                        return null
                    }
                }
            }

            return substitutor
        }
    }

    private fun buildSubstitutorFromTypeArguments(classSymbol: KaClassSymbol, classType: KaClassType): KaSubstitutor? {
        val typeParameters = classSymbol.typeParameters
        val typeArguments = classType.typeArguments
        if (typeParameters.size != typeArguments.size) return null

        val mappings = buildMap {
            for ((typeParameter, typeArgument) in typeParameters.zip(typeArguments)) {
                val concreteType = typeArgument.type ?: return null
                put(typeParameter, concreteType)
            }
        }

        return createSubstitutor(mappings)
    }
}
