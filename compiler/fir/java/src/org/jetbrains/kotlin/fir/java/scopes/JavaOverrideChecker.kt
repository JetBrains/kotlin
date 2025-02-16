/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.chooseIntersectionVisibilityOrNull
import org.jetbrains.kotlin.fir.scopes.impl.isAbstractAccordingToRawStatus
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptorRepresentation
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class JavaOverrideChecker internal constructor(
    private val session: FirSession,
    private val javaClassForTypeParameterStack: FirJavaClass?,
    private val baseScopes: List<FirTypeScope>?,
    private val considerReturnTypeKinds: Boolean,
) : FirAbstractOverrideChecker() {
    private val context: ConeTypeContext = session.typeContext
    private val javaTypeParameterStack: JavaTypeParameterStack
        get() = javaClassForTypeParameterStack?.javaTypeParameterStack ?: JavaTypeParameterStack.EMPTY

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
        substitutor: ConeSubstitutor,
        forceBoxCandidateType: Boolean,
        forceBoxBaseType: Boolean,
        dontComparePrimitivity: Boolean,
    ): Boolean {
        val candidateType = candidateTypeRef.toConeKotlinTypeProbablyFlexible(
            session, javaTypeParameterStack, candidateTypeRef.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        )
        val baseType = baseTypeRef.toConeKotlinTypeProbablyFlexible(
            session, javaTypeParameterStack, baseTypeRef.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        )

        val candidateTypeIsPrimitive = !forceBoxCandidateType && candidateType.isPrimitiveInJava(isReturnType = false)
        val baseTypeIsPrimitive = !forceBoxBaseType && baseType.isPrimitiveInJava(isReturnType = false)

        return (dontComparePrimitivity || candidateTypeIsPrimitive == baseTypeIsPrimitive) &&
                isEqualTypes(candidateType, baseType, substitutor)
    }

    // In most cases checking erasure of value parameters should be enough, but in some cases there might be semi-valid Java hierarchies
    // with same value parameters, but different return type kinds, so it's worth distinguishing them as different non-overridable members
    fun doesReturnTypesHaveSameKind(
        candidate: FirSimpleFunction,
        base: FirSimpleFunction,
    ): Boolean {
        val candidateTypeRef = candidate.returnTypeRef
        val baseTypeRef = base.returnTypeRef

        val candidateType = candidateTypeRef.toConeKotlinTypeProbablyFlexible(
            session, javaTypeParameterStack, candidateTypeRef.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        )
        val baseType = baseTypeRef.toConeKotlinTypeProbablyFlexible(
            session, javaTypeParameterStack, baseTypeRef.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        )

        val candidateHasPrimitiveReturnType = candidate.hasPrimitiveReturnTypeInJvm(candidateType)
        if (candidateHasPrimitiveReturnType != base.hasPrimitiveReturnTypeInJvm(baseType)) return false

        // Both candidate and base are not primitive
        if (!candidateHasPrimitiveReturnType) return true

        return candidateType.classLikeLookupTagIfAny == baseType.classLikeLookupTagIfAny
    }

    private fun ConeKotlinType.isPrimitiveInJava(isReturnType: Boolean): Boolean = with(context) {
        if (isNullableType() || CompilerConeAttributes.EnhancedNullability in attributes) return false

        val isVoid = isReturnType && isUnit
        return isPrimitiveOrNullablePrimitive || isVoid
    }

    private fun FirSimpleFunction.hasPrimitiveReturnTypeInJvm(returnType: ConeKotlinType): Boolean {
        if (!returnType.isPrimitiveInJava(isReturnType = true)) return false

        var foundNonPrimitiveOverridden = false

        baseScopes?.processOverriddenFunctions(symbol) {
            val type = it.fir.returnTypeRef.toConeKotlinTypeProbablyFlexible(
                session, javaTypeParameterStack, source?.fakeElement(KtFakeSourceElementKind.Enhancement)
            )
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
        val firstBound = symbol.fir.bounds.firstOrNull() // Note that in Java type parameter typed arguments always erased to first bound
        if (firstBound == null) {
            errorWithAttachment("Bound element is not found") {
                withFirEntry("typeParameterRef", it)
                val firTypeParameter = it.symbol.fir
                withFirEntry("typeParameter", firTypeParameter)
                withFirEntry("containingDeclaration", firTypeParameter.containingDeclarationSymbol.fir)
            }
        }

        symbol to firstBound.toConeKotlinTypeProbablyFlexible(
            session, javaTypeParameterStack, it.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        )
    }

    private fun FirTypeRef?.isTypeParameterDependent(): Boolean =
        this is FirResolvedTypeRef && coneType.isTypeParameterDependent()

    private fun ConeKotlinType.isTypeParameterDependent(): Boolean {
        if (this is ConeFlexibleType) return lowerBound.isTypeParameterDependent()
        if (this is ConeDefinitelyNotNullType) return original.isTypeParameterDependent()

        return this is ConeTypeParameterType || this is ConeClassLikeType && typeArguments.any { argument ->
            argument is ConeKotlinTypeProjection && argument.type.isTypeParameterDependent()
        }
    }

    private fun FirCallableDeclaration.isTypeParameterDependent(): Boolean =
        typeParameters.isNotEmpty() || returnTypeRef.isTypeParameterDependent() ||
                receiverParameter?.typeRef.isTypeParameterDependent() ||
                this is FirSimpleFunction && valueParameters.any { it.returnTypeRef.isTypeParameterDependent() }

    private fun FirTypeRef.extractTypeParametersTo(result: MutableCollection<FirTypeParameterRef>) {
        if (this is FirResolvedTypeRef) {
            coneType.extractTypeParametersTo(result)
        }
    }

    private fun ConeKotlinType.extractTypeParametersTo(result: MutableCollection<FirTypeParameterRef>) {
        when (this) {
            is ConeFlexibleType -> lowerBound.extractTypeParametersTo(result)
            is ConeDefinitelyNotNullType -> original.extractTypeParametersTo(result)
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
        receiverParameter?.typeRef?.extractTypeParametersTo(result)
        if (this is FirSimpleFunction) {
            this.valueParameters.forEach { it.returnTypeRef.extractTypeParametersTo(result) }
        }
    }

    override fun buildTypeParametersSubstitutorIfCompatible(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirCallableDeclaration
    ): ConeSubstitutor {
        overrideCandidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        baseDeclaration.lazyResolveToPhase(FirResolvePhase.TYPES)

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
        if (Visibilities.isPrivate(baseDeclaration.visibility)) return false

        overrideCandidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        baseDeclaration.lazyResolveToPhase(FirResolvePhase.TYPES)

        if (!overrideCandidate.hasSameValueParameterTypes(baseDeclaration)) {
            return false
        }

        if (overrideCandidate.origin == FirDeclarationOrigin.Java.Source && baseDeclaration.origin == FirDeclarationOrigin.Source) {
            // For override from Java source against the Kotlin base the following check of return type kinds is not important
            // From the other side, it can provoke problems in case baseDeclaration is from source and has an implicit return type
            // which is not yet resolved (see KT-57044)
            return true
        }

        // See test compiler/testData/compileKotlinAgainstCustomBinaries/incorrectJavaSignature
        // and relevant commit message (360d6741)
        if (considerReturnTypeKinds && !doesReturnTypesHaveSameKind(overrideCandidate, baseDeclaration)) {
            return false
        }

        return true
    }

    private fun FirSimpleFunction.hasSameValueParameterTypes(other: FirSimpleFunction): Boolean {
        // NB: 'this' is counted as a Java method that cannot have a receiver
        val otherValueParameterTypes = other.collectValueParameterTypes()
        val valueParameterTypes = valueParameters.map { it.returnTypeRef }
        if (valueParameterTypes.size != otherValueParameterTypes.size) return false

        val substitutor = buildTypeParametersSubstitutorIfCompatible(this, other)
        val forceBoxValueParameterType = forceSingleValueParameterBoxing(this)
        val forceBoxOtherValueParameterType = forceSingleValueParameterBoxing(other)
        val otherUnwrappedValueParameterTypes = other.unwrapFakeOverrides().collectValueParameterTypes()
        val unwrappedValueParameterTypes = unwrapFakeOverrides().valueParameters.map { it.returnTypeRef }

        for (i in valueParameterTypes.indices) {
            if (!isEqualTypes(
                    candidateTypeRef = valueParameterTypes[i],
                    baseTypeRef = otherValueParameterTypes[i],
                    substitutor = substitutor,
                    forceBoxCandidateType = forceBoxValueParameterType,
                    forceBoxBaseType = forceBoxOtherValueParameterType,
                    // This very hacky place is needed to match K1 logic
                    // See triangleWithFlexibleTypeAndSubstitution4.kt and neighbor tests
                    // The idea: normally in Java primitive type does not match non-primitive one
                    // However, if *both* types were constructed as generic substitutions,
                    // this check can (and should) be omitted
                    dontComparePrimitivity = otherUnwrappedValueParameterTypes.getOrNull(i)?.isTypeParameterDependent() == true &&
                            unwrappedValueParameterTypes.getOrNull(i)?.isTypeParameterDependent() == true,
                )
            ) return false
        }
        return true
    }

    private fun FirSimpleFunction.collectValueParameterTypes(): List<FirTypeRef> {
        return buildList {
            contextParameters.mapTo(this) { it.returnTypeRef }
            receiverParameter?.typeRef?.let { add(it) }
            valueParameters.mapTo(this) { it.returnTypeRef }
        }
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean {
        if (baseDeclaration.modality == Modality.FINAL) return false
        if (Visibilities.isPrivate(baseDeclaration.visibility)) return false

        overrideCandidate.lazyResolveToPhase(FirResolvePhase.TYPES)
        baseDeclaration.lazyResolveToPhase(FirResolvePhase.TYPES)

        val receiverTypeRef = baseDeclaration.receiverParameter?.typeRef
        return when (overrideCandidate) {
            is FirSimpleFunction -> {
                if (receiverTypeRef == null) {
                    // TODO: setters
                    return overrideCandidate.valueParameters.isEmpty()
                } else {
                    if (overrideCandidate.valueParameters.size != 1) return false
                    return isEqualTypes(
                        receiverTypeRef, overrideCandidate.valueParameters.single().returnTypeRef, ConeSubstitutor.Empty,
                        forceBoxCandidateType = false, forceBoxBaseType = false,
                        dontComparePrimitivity = false,
                    )
                }
            }
            is FirProperty -> {
                val overrideReceiverTypeRef = overrideCandidate.receiverParameter?.typeRef
                return when {
                    receiverTypeRef == null -> overrideReceiverTypeRef == null
                    overrideReceiverTypeRef == null -> false
                    else -> isEqualTypes(
                        receiverTypeRef, overrideReceiverTypeRef, ConeSubstitutor.Empty,
                        forceBoxCandidateType = false, forceBoxBaseType = false,
                        dontComparePrimitivity = false,
                    )
                }
            }
            else -> false
        }
    }

    // Boxing is only necessary for 'remove(E): Boolean' of a MutableCollection<Int> implementation.
    // Otherwise this method might clash with 'remove(I): E' defined in the java.util.List JDK interface (mapped to kotlin 'removeAt').
    // As in the K1 implementation in `methodSignatureMapping.kt`, we're checking if the method has `MutableCollection.remove`
    // in its overridden symbols.
    private fun forceSingleValueParameterBoxing(function: FirSimpleFunction): Boolean {
        if (function.name.asString() != "remove" || function.receiverParameter != null || function.contextParameters.isNotEmpty())
            return false

        val parameter = function.valueParameters.singleOrNull() ?: return false

        val parameterConeType = parameter.returnTypeRef.toConeKotlinTypeProbablyFlexible(
            session, javaTypeParameterStack, function.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        )
        if (!parameterConeType.fullyExpandedType(session).lowerBoundIfFlexible().isInt) return false

        var overridesMutableCollectionRemove = false

        baseScopes?.processOverriddenFunctions(function.symbol) {
            if (it.fir.containingClassLookupTag() == StandardClassIds.MutableCollection.toLookupTag()) {
                overridesMutableCollectionRemove = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        return overridesMutableCollectionRemove
    }

    override fun chooseIntersectionVisibility(
        overrides: Collection<FirCallableSymbol<*>>,
        dispatchClassSymbol: FirRegularClassSymbol?,
    ): Visibility {
        // In Java it's OK to inherit multiple implementations of the same function
        // from the supertypes as long as there's an implementation from a class.
        // We shouldn't reject green Java code.
        if (dispatchClassSymbol?.fir is FirJavaClass) {
            val nonAbstractFromClass = overrides.find {
                !it.isAbstractAccordingToRawStatus && it.dispatchReceiverClassLookupTagOrNull()
                    ?.toSymbol(session)?.classKind == ClassKind.CLASS
            }
            if (nonAbstractFromClass != null) {
                return nonAbstractFromClass.rawStatus.visibility
            }
        }

        return chooseIntersectionVisibilityOrNull(overrides) ?: Visibilities.Unknown
    }
}
