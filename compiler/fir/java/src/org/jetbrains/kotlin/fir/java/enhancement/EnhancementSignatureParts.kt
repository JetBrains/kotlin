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
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeWithoutEnhancement
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.JavaDefaultQualifiers
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.FqNameUnsafe
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
        predefined: TypeEnhancementInfo? = null,
        forAnnotationMember: Boolean = false
    ): FirResolvedTypeRef {
        val typeWithoutEnhancement = current.type
            .toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, forAnnotationMember)
        val qualifiers = computeIndexedQualifiersForOverride(typeWithoutEnhancement, predefined)
        return buildResolvedTypeRef {
            type = typeWithoutEnhancement.enhance(session, qualifiers) ?: typeWithoutEnhancement
            annotations += current.annotations
        }
    }

    private fun FirTypeRef.toConeKotlinType(session: FirSession): ConeKotlinType? =
        when (this) {
            is FirResolvedTypeRef -> type
            is FirJavaTypeRef -> type.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
            else -> null
        }

    private fun ConeKotlinType?.toIndexed(context: FirJavaEnhancementContext): List<TypeAndDefaultQualifiers> {
        val list = ArrayList<TypeAndDefaultQualifiers>(1)

        fun add(type: ConeKotlinType?) {
            // TODO: should use the context from parent type
            val annotations = type?.attributes?.customAnnotations.orEmpty()
            val c = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, annotations)
            list.add(TypeAndDefaultQualifiers(type, c.defaultTypeQualifiers?.get(AnnotationQualifierApplicabilityType.TYPE_USE)))
            type?.typeArguments?.forEach { add(it.type) }
        }

        add(this)
        return list
    }

    private fun ConeKotlinType.extractQualifiers(): JavaTypeQualifiers {
        val lower = lowerBoundIfFlexible()
        val upper = upperBoundIfFlexible()
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

    private fun composeAnnotations(first: List<FirAnnotationCall>, second: List<FirAnnotationCall>): List<FirAnnotationCall> {
        return when {
            first.isEmpty() -> second
            second.isEmpty() -> first
            else -> first + second
        }
    }

    private fun TypeAndDefaultQualifiers.extractQualifiersFromAnnotations(isHeadTypeConstructor: Boolean): JavaTypeQualifiers {
        val annotations = type?.attributes?.customAnnotations.orEmpty()
        val composedAnnotations =
            if (isHeadTypeConstructor && typeContainer != null)
                composeAnnotations(typeContainer.annotations, annotations)
            else
                annotations

        val defaultTypeQualifier =
            if (isHeadTypeConstructor)
                context.defaultTypeQualifiers?.get(containerApplicabilityType)
            else
                defaultQualifiers

        val nullabilityInfo = typeQualifierResolver.extractNullability(composedAnnotations)
            ?: defaultTypeQualifier?.nullabilityQualifier

        return JavaTypeQualifiers(
            nullabilityInfo?.qualifier,
            typeQualifierResolver.extractMutability(composedAnnotations),
            isNotNullTypeParameter = nullabilityInfo?.qualifier == NullabilityQualifier.NOT_NULL &&
                    type?.lowerBoundIfFlexible() is ConeTypeParameterType,
            isNullabilityQualifierForWarning = nullabilityInfo?.isForWarningOnly == true
        )
    }

    private fun computeIndexedQualifiersForOverride(current: ConeKotlinType?, predefined: TypeEnhancementInfo?): IndexedJavaTypeQualifiers {
        val indexedFromSupertypes = fromOverridden.map { it.toConeKotlinType(context.session).toIndexed(context) }
        val indexedThisType = current.toIndexed(context)

        // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
        // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
        // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
        // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
        // Note that `this` is flexible here, so it's equal to it's bounds
        val onlyHeadTypeConstructor = isCovariant && fromOverridden.any { true /*equalTypes(it, this)*/ }

        val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
        val computedResult = Array(treeSize) { index ->
            val qualifiers = indexedThisType[index].extractQualifiersFromAnnotations(index == 0)
            val superQualifiers = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type?.extractQualifiers() }
            qualifiers.computeQualifiersForOverride(superQualifiers, index == 0 && isCovariant, index == 0 && isForVarargParameter)
        }
        return { index -> predefined?.map?.get(index) ?: computedResult.getOrNull(index) ?: JavaTypeQualifiers.NONE }
    }

    private data class TypeAndDefaultQualifiers(
        val type: ConeKotlinType?, // null denotes '*' here
        val defaultQualifiers: JavaDefaultQualifiers?
    )
}
