/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeWithoutEnhancement
import org.jetbrains.kotlin.fir.java.toFirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.MUTABLE_ANNOTATIONS
import org.jetbrains.kotlin.load.java.READ_ONLY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class EnhancementSignatureParts(
    private val typeQualifierResolver: FirAnnotationTypeQualifierResolver,
    private val typeContainer: FirAnnotationContainer?,
    private val javaTypeParameterStack: JavaTypeParameterStack,
    private val current: FirJavaTypeRef,
    private val fromOverridden: Collection<FirTypeRef>,
    private val isCovariant: Boolean,
    private val context: FirJavaEnhancementContext,
    private val containerApplicabilityType: AnnotationQualifierApplicabilityType
) {
    private val isForVarargParameter get() = typeContainer.safeAs<FirValueParameter>()?.isVararg == true

    private fun ConeKotlinType.toFqNameUnsafe(): FqNameUnsafe? =
        ((this as? ConeLookupTagBasedType)?.lookupTag as? ConeClassLikeLookupTag)?.classId?.asSingleFqName()?.toUnsafe()

    internal fun enhance(
        session: FirSession,
        jsr305State: Jsr305State,
        predefined: TypeEnhancementInfo? = null,
        forAnnotationValueParameter: Boolean = false
    ): PartEnhancementResult {
        val qualifiers = computeIndexedQualifiersForOverride(session, jsr305State)

        val qualifiersWithPredefined = predefined?.let {
            IndexedJavaTypeQualifiers(qualifiers.size) { index ->
                predefined.map[index] ?: qualifiers(index)
            }
        }

        val typeWithoutEnhancement = current.type.toConeKotlinTypeWithoutEnhancement(
            session,
            javaTypeParameterStack,
            forAnnotationValueParameter
        )
        val containsFunctionN = typeWithoutEnhancement.contains {
            if (it is ConeClassErrorType) false
            else {
                val classId = it.lookupTag.classId
                classId.shortClassName == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME.shortName() &&
                        classId.asSingleFqName() == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME
            }
        }

        val enhancedCurrent = current.enhance(
            session, qualifiersWithPredefined ?: qualifiers,
            typeWithoutEnhancement
        )
        return PartEnhancementResult(
            enhancedCurrent, wereChanges = true, containsFunctionN = containsFunctionN
        )
    }

    private fun ConeKotlinType.contains(isSpecialType: (ConeClassLikeType) -> Boolean): Boolean {
        return when (this) {
            is ConeClassLikeType -> isSpecialType(this)
            else -> false
        }
    }


    private fun FirTypeRef.toIndexed(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        jsr305State: Jsr305State,
        context: FirJavaEnhancementContext
    ): List<TypeAndDefaultQualifiers> {
        val list = ArrayList<TypeAndDefaultQualifiers>(1)

        fun add(type: FirTypeRef?) {
            val c = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, type?.annotations.orEmpty())

            list.add(
                TypeAndDefaultQualifiers(
                    type,
                    c.defaultTypeQualifiers
                        ?.get(AnnotationQualifierApplicabilityType.TYPE_USE)
                )
            )

            if (type is FirJavaTypeRef) {
                for (arg in type.type.typeArguments()) {
                    if (arg is JavaWildcardType || arg == null) {
                        add(null)
                    } else {
                        add(arg.toFirJavaTypeRef(context.session, javaTypeParameterStack))
                    }
                }
            } else if (type != null) {
                for (arg in type.typeArguments()) {
                    if (arg is FirStarProjection) {
                        add(null)
                    } else if (arg is FirTypeProjectionWithVariance) {
                        add(arg.typeRef)
                    }
                }
            }
        }

        add(this)
        return list
    }

    private fun extractQualifiers(lower: ConeKotlinType, upper: ConeKotlinType): JavaTypeQualifiers {
        val mapping = JavaToKotlinClassMap
        return JavaTypeQualifiers(
            when {
                lower.isMarkedNullable -> NullabilityQualifier.NULLABLE
                !upper.isMarkedNullable -> NullabilityQualifier.NOT_NULL
                else -> null
            },
            when {
                mapping.isReadOnly(lower.toFqNameUnsafe()) -> MutabilityQualifier.READ_ONLY
                mapping.isMutable(upper.toFqNameUnsafe()) -> MutabilityQualifier.MUTABLE
                else -> null
            },
            isNotNullTypeParameter = lower is ConeDefinitelyNotNullType
        )
    }

    private fun FirTypeRef.extractQualifiers(session: FirSession): JavaTypeQualifiers {
        val (lower, upper) = when (this) {
            is FirResolvedTypeRef -> {
                val type = this.type
                if (type is ConeFlexibleType) {
                    Pair(type.lowerBound, type.upperBound)
                } else {
                    Pair(type, type)
                }
            }
            is FirJavaTypeRef -> {
                val convertedType = type.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
                Pair(
                    convertedType.lowerBoundIfFlexible(),
                    convertedType.upperBoundIfFlexible()
                )
            }
            else -> return JavaTypeQualifiers.NONE
        }

        return extractQualifiers(lower, upper)
    }

    private fun composeAnnotations(first: List<FirAnnotationCall>, second: List<FirAnnotationCall>): List<FirAnnotationCall> {
        return when {
            first.isEmpty() -> second
            second.isEmpty() -> first
            else -> first + second
        }
    }

    private fun FirTypeRef?.extractQualifiersFromAnnotations(
        isHeadTypeConstructor: Boolean,
        defaultQualifiersForType: JavaTypeQualifiers?,
        jsr305State: Jsr305State
    ): JavaTypeQualifiers {
        val composedAnnotation =
            if (isHeadTypeConstructor && typeContainer != null)
                composeAnnotations(typeContainer.annotations, this?.annotations.orEmpty())
            else
                this?.annotations.orEmpty()

        fun <T : Any> List<ClassId>.ifPresent(qualifier: T) =
            if (any { classId ->
                    composedAnnotation.any { it.classId == classId }
                }
            ) qualifier else null

        fun <T : Any> uniqueNotNull(x: T?, y: T?) = if (x == null || y == null || x == y) x ?: y else null

        val defaultTypeQualifier =
            if (isHeadTypeConstructor)
                context.defaultTypeQualifiers?.get(containerApplicabilityType)
            else
                defaultQualifiersForType

        val nullabilityInfo = composedAnnotation.extractNullability(typeQualifierResolver, jsr305State)
            ?: defaultTypeQualifier?.nullability?.let { nullability ->
                NullabilityQualifierWithMigrationStatus(
                    nullability,
                    defaultTypeQualifier.isNullabilityQualifierForWarning
                )
            }

        @Suppress("SimplifyBooleanWithConstants")
        return JavaTypeQualifiers(
            nullabilityInfo?.qualifier,
            uniqueNotNull(
                READ_ONLY_ANNOTATION_IDS.ifPresent(
                    MutabilityQualifier.READ_ONLY
                ),
                MUTABLE_ANNOTATION_IDS.ifPresent(
                    MutabilityQualifier.MUTABLE
                )
            ),
            isNotNullTypeParameter = nullabilityInfo?.qualifier == NullabilityQualifier.NOT_NULL && this.isTypeParameterBasedType(),
            isNullabilityQualifierForWarning = nullabilityInfo?.isForWarningOnly == true
        )
    }

    private fun FirTypeRef?.isTypeParameterBasedType() =
        ((this as? FirJavaTypeRef)?.type as? JavaClassifierType)?.classifier is JavaTypeParameter

    private fun FirTypeRef?.computeQualifiersForOverride(
        session: FirSession,
        fromSupertypes: Collection<FirTypeRef>,
        defaultQualifiersForType: JavaTypeQualifiers?,
        isHeadTypeConstructor: Boolean,
        jsr305State: Jsr305State
    ): JavaTypeQualifiers {
        val superQualifiers = fromSupertypes.map { it.extractQualifiers(session) }
        val mutabilityFromSupertypes = superQualifiers.mapNotNull { it.mutability }.toSet()
        val nullabilityFromSupertypes = superQualifiers.mapNotNull { it.nullability }.toSet()
        val nullabilityFromSupertypesWithWarning = fromOverridden
            .mapNotNull { it.extractQualifiers(session).nullability }
            .toSet()

        val own = extractQualifiersFromAnnotations(isHeadTypeConstructor, defaultQualifiersForType, jsr305State)
        val ownNullability = own.takeIf { !it.isNullabilityQualifierForWarning }?.nullability
        val ownNullabilityForWarning = own.nullability

        val isCovariantPosition = isCovariant && isHeadTypeConstructor
        val nullability =
            nullabilityFromSupertypes.select(ownNullability, isCovariantPosition)
                // Vararg value parameters effectively have non-nullable type in Kotlin
                // and having nullable types in Java may lead to impossibility of overriding them in Kotlin
                ?.takeUnless { isForVarargParameter && isHeadTypeConstructor && it == NullabilityQualifier.NULLABLE }

        val mutability =
            mutabilityFromSupertypes
                .select(MutabilityQualifier.MUTABLE, MutabilityQualifier.READ_ONLY, own.mutability, isCovariantPosition)

        val canChange = ownNullabilityForWarning != ownNullability || nullabilityFromSupertypesWithWarning != nullabilityFromSupertypes
        val isAnyNonNullTypeParameter = own.isNotNullTypeParameter || superQualifiers.any { it.isNotNullTypeParameter }
        if (nullability == null && canChange) {
            val nullabilityWithWarning =
                nullabilityFromSupertypesWithWarning.select(ownNullabilityForWarning, isCovariantPosition)

            return createJavaTypeQualifiers(
                nullabilityWithWarning, mutability,
                forWarning = true, isAnyNonNullTypeParameter = isAnyNonNullTypeParameter
            )
        }

        return createJavaTypeQualifiers(
            nullability, mutability,
            forWarning = nullability == null,
            isAnyNonNullTypeParameter = isAnyNonNullTypeParameter
        )
    }

    private fun computeIndexedQualifiersForOverride(session: FirSession, jsr305State: Jsr305State): IndexedJavaTypeQualifiers {
        val indexedFromSupertypes = fromOverridden.map { it.toIndexed(typeQualifierResolver, jsr305State, context) }
        val indexedThisType = current.toIndexed(typeQualifierResolver, jsr305State, context)

        // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
        // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
        // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
        // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
        // Note that `this` is flexible here, so it's equal to it's bounds
        val onlyHeadTypeConstructor = isCovariant && fromOverridden.any { true /*equalTypes(it, this)*/ }

        val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
        val computedResult = Array(treeSize) { index ->
            val isHeadTypeConstructor = index == 0
            assert(isHeadTypeConstructor || !onlyHeadTypeConstructor) { "Only head type constructors should be computed" }

            val (type, defaultQualifiers) = indexedThisType[index]
            val verticalSlice = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type }

            // Only the head type constructor is safely co-variant
            type.computeQualifiersForOverride(session, verticalSlice, defaultQualifiers, isHeadTypeConstructor, jsr305State)
        }

        return IndexedJavaTypeQualifiers(computedResult)
    }

    @Suppress("unused")
    internal open class PartEnhancementResult(
        val type: FirResolvedTypeRef,
        val wereChanges: Boolean,
        val containsFunctionN: Boolean
    )

    companion object {
        private val READ_ONLY_ANNOTATION_IDS = READ_ONLY_ANNOTATIONS.map { ClassId.topLevel(it) }
        private val MUTABLE_ANNOTATION_IDS = MUTABLE_ANNOTATIONS.map { ClassId.topLevel(it) }
    }
}

