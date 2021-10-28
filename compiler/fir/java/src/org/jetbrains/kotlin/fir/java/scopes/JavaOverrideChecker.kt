/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractOverrideChecker
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptorRepresentation
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

class JavaOverrideChecker internal constructor(
    private val session: FirSession,
    private val javaTypeParameterStack: JavaTypeParameterStack,
    private val baseScope: FirTypeScope?,
    private val considerReturnTypeKinds: Boolean,
) : FirAbstractOverrideChecker() {
    private val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(
        candidateType: ConeKotlinType,
        baseType: ConeKotlinType,
        substitutor: ConeSubstitutor
    ): Boolean {
        if (candidateType is ConeRawType) {
            return candidateType.computeJvmDescriptorRepresentation() == baseType.computeJvmDescriptorRepresentation()
        }

        if (candidateType is ConeFlexibleType) return isEqualTypes(candidateType.lowerBound, baseType, substitutor)
        if (baseType is ConeFlexibleType) return isEqualTypes(candidateType, baseType.lowerBound, substitutor)
        if (candidateType is ConeClassLikeType && baseType is ConeClassLikeType) {
            val candidateTypeClassId = candidateType.fullyExpandedType(session).lookupTag.classId.let { it.readOnlyToMutable() ?: it }
            val baseTypeClassId = baseType.fullyExpandedType(session).lookupTag.classId.let { it.readOnlyToMutable() ?: it }
            if (candidateTypeClassId != baseTypeClassId) return false
            if (candidateTypeClassId == StandardClassIds.Array) {
                assert(candidateType.typeArguments.size == 1) {
                    "Array type with unexpected number of type arguments: $candidateType"
                }
                assert(baseType.typeArguments.size == 1) {
                    "Array type with unexpected number of type arguments: $baseType"
                }
                return isEqualArrayElementTypeProjections(
                    candidateType.typeArguments.single(),
                    baseType.typeArguments.single(),
                    substitutor
                )
            }
            return true
        }
        return with(context) {
            areEqualTypeConstructors(
                substitutor.substituteOrSelf(candidateType).typeConstructor(),
                substitutor.substituteOrSelf(baseType).typeConstructor()
            )
        }
    }

    private fun isEqualTypes(
        candidateTypeRef: FirTypeRef,
        baseTypeRef: FirTypeRef,
        substitutor: ConeSubstitutor
    ): Boolean {
        val candidateType = candidateTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)
        val baseType = baseTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)

        if (candidateType.isPrimitiveInJava(isReturnType = false) != baseType.isPrimitiveInJava(isReturnType = false)) return false

        return isEqualTypes(
            candidateType,
            baseType,
            substitutor
        )
    }

    // In most cases checking erasure of value parameters should be enough, but in some cases there might be semi-valid Java hierarchies
    // with same value parameters, but different return type kinds, so it's worth distinguishing them as different non-overridable members
    private fun doesReturnTypesHaveSameKind(
        candidate: FirSimpleFunction,
        base: FirSimpleFunction,
    ): Boolean {
        val candidateTypeRef = candidate.returnTypeRef
        val baseTypeRef = base.returnTypeRef

        val candidateType = candidateTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)
        val baseType = baseTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)

        val candidateHasPrimitiveReturnType = candidate.hasPrimitiveReturnTypeInJvm(candidateType)
        if (candidateHasPrimitiveReturnType != base.hasPrimitiveReturnTypeInJvm(baseType)) return false

        // Both candidate and base are not primitive
        if (!candidateHasPrimitiveReturnType) return true

        return (candidateType as? ConeClassLikeType)?.lookupTag == (baseType as? ConeClassLikeType)?.lookupTag
    }

    private fun ConeKotlinType.isPrimitiveInJava(isReturnType: Boolean): Boolean = with(context) {
        if (isNullableType() || CompilerConeAttributes.EnhancedNullability in attributes) return false

        val isVoid = isReturnType && isUnit
        return isPrimitiveOrNullablePrimitive || isVoid
    }

    private fun FirSimpleFunction.hasPrimitiveReturnTypeInJvm(returnType: ConeKotlinType): Boolean {
        if (!returnType.isPrimitiveInJava(isReturnType = true)) return false

        var foundNonPrimitiveOverridden = false

        baseScope?.processOverriddenFunctions(symbol) {
            val type = it.fir.returnTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)
            if (!type.isPrimitiveInJava(isReturnType = true)) {
                foundNonPrimitiveOverridden = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        return !foundNonPrimitiveOverridden
    }

    private fun isEqualArrayElementTypeProjections(
        candidateTypeProjection: ConeTypeProjection,
        baseTypeProjection: ConeTypeProjection,
        substitutor: ConeSubstitutor
    ): Boolean =
        when {
            candidateTypeProjection is ConeKotlinTypeProjection && baseTypeProjection is ConeKotlinTypeProjection ->
                isEqualTypes(candidateTypeProjection.type, baseTypeProjection.type, substitutor)
            candidateTypeProjection is ConeStarProjection && baseTypeProjection is ConeStarProjection -> true
            else -> false
        }

    private fun Collection<FirTypeParameterRef>.buildErasure() = associate {
        val symbol = it.symbol
        val firstBound = symbol.fir.bounds.first() // Note that in Java type parameter typed arguments always erased to first bound
        symbol to firstBound.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)
    }

    private fun FirTypeRef?.isTypeParameterDependent(): Boolean =
        this is FirResolvedTypeRef && type.lowerBoundIfFlexible().isTypeParameterDependent()

    private fun ConeKotlinType.isTypeParameterDependent(): Boolean =
        this is ConeTypeParameterType || this is ConeClassLikeType && typeArguments.any { argument ->
            argument is ConeKotlinTypeProjection && argument.type.isTypeParameterDependent()
        }

    private fun FirCallableDeclaration.isTypeParameterDependent(): Boolean =
        typeParameters.isNotEmpty() || returnTypeRef.isTypeParameterDependent() ||
                receiverTypeRef.isTypeParameterDependent() ||
                this is FirSimpleFunction && valueParameters.any { it.returnTypeRef.isTypeParameterDependent() }

    private fun FirTypeRef.extractTypeParametersTo(result: MutableCollection<FirTypeParameterRef>) {
        if (this is FirResolvedTypeRef) {
            type.lowerBoundIfFlexible().extractTypeParametersTo(result)
        }
    }

    private fun ConeKotlinType.extractTypeParametersTo(result: MutableCollection<FirTypeParameterRef>) {
        when (this) {
            is ConeTypeParameterType -> {
                result += lookupTag.typeParameterSymbol.fir
            }
            is ConeClassLikeType -> typeArguments.forEach {
                if (it is ConeKotlinTypeProjection) {
                    it.type.extractTypeParametersTo(result)
                }
            }
            else -> {
            }
        }
    }

    private fun FirCallableDeclaration.extractTypeParametersTo(result: MutableCollection<FirTypeParameterRef>) {
        result += typeParameters
        returnTypeRef.extractTypeParametersTo(result)
        receiverTypeRef?.extractTypeParametersTo(result)
        if (this is FirSimpleFunction) {
            this.valueParameters.forEach { it.returnTypeRef.extractTypeParametersTo(result) }
        }
    }

    override fun buildTypeParametersSubstitutorIfCompatible(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirCallableDeclaration
    ): ConeSubstitutor {
        overrideCandidate.ensureResolved(FirResolvePhase.TYPES)
        baseDeclaration.ensureResolved(FirResolvePhase.TYPES)

        if (!overrideCandidate.isTypeParameterDependent() && !baseDeclaration.isTypeParameterDependent()) {
            return ConeSubstitutor.Empty
        }
        val typeParameters = linkedSetOf<FirTypeParameterRef>()
        overrideCandidate.extractTypeParametersTo(typeParameters)
        baseDeclaration.extractTypeParametersTo(typeParameters)
        return substitutorByMap(typeParameters.buildErasure(), session)
    }

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        if (overrideCandidate.isStatic != baseDeclaration.isStatic) return false

        overrideCandidate.ensureResolved(FirResolvePhase.TYPES)
        baseDeclaration.ensureResolved(FirResolvePhase.TYPES)

        // NB: overrideCandidate is from Java and has no receiver
        val receiverTypeRef = baseDeclaration.receiverTypeRef
        val baseParameterTypes = listOfNotNull(receiverTypeRef) + baseDeclaration.valueParameters.map { it.returnTypeRef }

        if (overrideCandidate.valueParameters.size != baseParameterTypes.size) return false
        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration)

        if (!overrideCandidate.valueParameters.zip(baseParameterTypes).all { (paramFromJava, baseType) ->
                isEqualTypes(paramFromJava.returnTypeRef, baseType, substitutor)
            }) {
            return false
        }

        if (considerReturnTypeKinds && !doesReturnTypesHaveSameKind(overrideCandidate, baseDeclaration)) return false

        return true
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean {
        if (baseDeclaration.modality == Modality.FINAL) return false

        overrideCandidate.ensureResolved(FirResolvePhase.TYPES)
        baseDeclaration.ensureResolved(FirResolvePhase.TYPES)

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

}
