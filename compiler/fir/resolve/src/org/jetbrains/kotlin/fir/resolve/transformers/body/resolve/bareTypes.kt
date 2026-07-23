/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleBareInferenceFailed
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.canHaveUndefinedNullability
import org.jetbrains.kotlin.types.model.makeDefinitelyNotNullOrNotNull
import org.jetbrains.kotlin.types.model.replaceType
import org.jetbrains.kotlin.types.model.withNullability


context(onDiagnostic: (ConeDiagnostic) -> Unit)
fun BodyResolveComponents.computeRepresentativeTypeForBareType(type: ConeClassLikeType, originalType: ConeKotlinType): ConeKotlinType? {
    originalType.lowerBoundIfFlexible().fullyExpandedType().let {
        if (it !== originalType) return computeRepresentativeTypeForBareType(type, it)
    }

    if (originalType is ConeIntersectionType) {
        val candidatesFromIntersectedTypes = originalType.intersectedTypes.mapNotNull { computeRepresentativeTypeForBareType(type, it) }
        candidatesFromIntersectedTypes.firstOrNull { it.typeArguments.isNotEmpty() }?.let { return it }
        return candidatesFromIntersectedTypes.firstOrNull()
    }

    session.typeApproximator.approximateToSuperType(
        originalType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
    )?.let {
        return computeRepresentativeTypeForBareType(type, it)
    }

    val originalClassLookupTag = originalType.fullyExpandedType().classLikeLookupTagIfAny ?: return null

    val castTypeAlias = type.abbreviatedTypeOrSelf.classLikeLookupTagIfAny?.toTypeAliasSymbol()?.fir
    if (castTypeAlias != null && !canBeUsedAsBareType(castTypeAlias)) return null

    val expandedCastType = type.fullyExpandedType()
    val castClass = expandedCastType.lookupTag.toRegularClassSymbol()?.fir ?: return null

    val superTypeWithParameters = with(session.typeContext) {
        val correspondingSupertype = AbstractTypeChecker.findCorrespondingSupertypes(
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false),
            castClass.defaultType(), originalClassLookupTag,
        ).firstOrNull() as? ConeClassLikeType ?: return null

        if (originalType.isMarkedNullable) correspondingSupertype.withNullability(nullable = true) as ConeClassLikeType
        else correspondingSupertype
    }

    val substitution = trySimpleInference(castClass, originalType, superTypeWithParameters, onDiagnostic) ?: run {
        onDiagnostic(
            ConeSimpleBareInferenceFailed(
                "originalType: $originalType; castClass: ${castClass.defaultType()}, superTypeWithParameters: $superTypeWithParameters"
            )
        )
        tryLegacyInference(castClass, originalType, superTypeWithParameters)
    } ?: return null

    val newArguments = castClass.typeParameters.map { substitution[it.symbol] ?: return@computeRepresentativeTypeForBareType null }
    return expandedCastType.withArguments(newArguments.toTypedArray())
}


private fun BodyResolveComponents.tryLegacyInference(
    castClass: FirRegularClass,
    originalType: ConeKotlinType,
    superTypeWithParameters: ConeClassLikeType,
): Map<FirTypeParameterSymbol, ConeTypeProjection>? {
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
    val typeParameters = castClass.typeParameters.mapTo(mutableSetOf()) { it.symbol }
    if (!session.doUnify(originalType, superTypeWithParameters, typeParameters, substitution)) return null
    return substitution
}

data class SimpleInferenceStats(
    val containingArguments: Int,
    val directInheritance: Int,
    val isSameConstraints: Boolean?,
    val isSameVariance: Boolean?,
    val isOriginalUnconstrained: Boolean?,
    val isOriginalSatisfiesSubtypeConstraints: Boolean?,
)

private fun BodyResolveComponents.trySimpleInference(
    castClass: FirRegularClass,
    originalType: ConeKotlinType,
    superTypeWithParameters: ConeClassLikeType,
    onDiagnostic: (ConeDiagnostic) -> Unit,
): Map<FirTypeParameterSymbol, ConeTypeProjection>? {
    val typeParameters = castClass.typeParameters.mapTo(mutableSetOf()) { it.symbol }
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
    val originalArguments = originalType.typeArguments
    val supertypeArguments = superTypeWithParameters.typeArguments

    typeParameters.forEach { typeParameter ->
        val containingArguments = supertypeArguments.zip(supertypeArguments.indices).filter { [argumentType, idx] ->
            argumentType.type?.contains { it is ConeTypeParameterType && it.lookupTag.typeParameterSymbol == typeParameter } ?: false
        }

        val directInheritanceArguments = containingArguments.filter { [argumentType, idx] ->
            argumentType is ConeTypeParameterType && argumentType.lookupTag.typeParameterSymbol == typeParameter
        }

        if (directInheritanceArguments.size != 1) {
            val stats = SimpleInferenceStats(containingArguments.size, directInheritanceArguments.size, null, null, null, null)
            onDiagnostic(ConeSimpleBareInferenceFailed(stats.toString()))
        }

        val [type, idx] = directInheritanceArguments.single()
    }

    for (i in originalArguments.indices) {
        val originalArgument = originalArguments[i]
        val supertypeArgument = supertypeArguments[i]
        val typeParameterType = supertypeArgument as? ConeTypeParameterType ?: continue
        if (typeParameterType.isMarkedNullable) return null
        val typeParameterSymbol = typeParameterType.lookupTag.typeParameterSymbol
        if (typeParameterSymbol !in typeParameters || typeParameterSymbol in substitution) return null
        substitution[typeParameterSymbol] = originalArgument
    }
    if (substitution.size != typeParameters.size) return null
    return substitution
}

private fun canBeUsedAsBareType(firTypeAlias: FirTypeAlias): Boolean {
    firTypeAlias.lazyResolveToPhase(FirResolvePhase.TYPES)

    val typeAliasParameters = firTypeAlias.typeParameters.toSet()
    val usedTypeParameters = mutableSetOf<FirTypeParameter>()

    val expandedType = firTypeAlias.expandedConeType ?: return false
    for (argument in expandedType.typeArguments) {
        if (argument.kind == ProjectionKind.STAR) continue
        if (argument.kind != ProjectionKind.INVARIANT) return false

        val type = argument.type!!
        val typeParameter = (type as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.fir ?: return false
        if (typeParameter !in typeAliasParameters || typeParameter in usedTypeParameters) return false

        usedTypeParameters.add(typeParameter)
    }

    return true
}

