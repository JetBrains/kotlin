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
import org.jetbrains.kotlin.fir.java.toFirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.JavaDefaultQualifiers
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType
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

    private val attributesCache = mutableMapOf<FirTypeRef?, ConeAttributes>().withDefault { ConeAttributes.Empty }

    private fun ConeKotlinType.toFqNameUnsafe(): FqNameUnsafe? =
        ((this as? ConeLookupTagBasedType)?.lookupTag as? ConeClassLikeLookupTag)?.classId?.asSingleFqName()?.toUnsafe()

    internal fun enhance(
        session: FirSession,
        predefined: TypeEnhancementInfo? = null,
        forAnnotationMember: Boolean = false
    ): PartEnhancementResult {
        val qualifiers = computeIndexedQualifiersForOverride(session)

        val qualifiersWithPredefined = predefined?.let {
            IndexedJavaTypeQualifiers(qualifiers.size) { index ->
                predefined.map[index] ?: qualifiers(index)
            }
        }

        val typeWithoutEnhancement = current.type.toConeKotlinTypeWithoutEnhancement(
            session,
            javaTypeParameterStack,
            forAnnotationMember,
            attributes = attributesCache.getValue(current)
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
        context: FirJavaEnhancementContext
    ): List<TypeAndDefaultQualifiers> {
        val list = ArrayList<TypeAndDefaultQualifiers>(1)

        fun add(type: FirTypeRef?) {
            // TODO: should use the context from parent type
            val c = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, type?.annotations.orEmpty())
            list.add(TypeAndDefaultQualifiers(type, c.defaultTypeQualifiers?.get(AnnotationQualifierApplicabilityType.TYPE_USE)))

            when (type) {
                is FirJavaTypeRef -> {
                    for (arg in type.type.typeArguments()) {
                        add(arg.takeIf { it !is JavaWildcardType }?.toFirJavaTypeRef(context.session, javaTypeParameterStack))
                    }
                }
                is FirUserTypeRef -> {
                    for (arg in type.qualifier.lastOrNull()?.typeArgumentList?.typeArguments.orEmpty()) {
                        add((arg as? FirTypeProjectionWithVariance)?.typeRef)
                    }
                }
                is FirResolvedTypeRef -> {
                    for (arg in type.type.typeArguments) {
                        add(arg.type?.let { buildResolvedTypeRef { this.type = it } })
                    }
                }
                else -> Unit
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
        defaultQualifiersForType: JavaDefaultQualifiers?
    ): JavaTypeQualifiers {
        val composedAnnotation =
            if (isHeadTypeConstructor && typeContainer != null)
                composeAnnotations(typeContainer.annotations, this?.annotations.orEmpty())
            else
                this?.annotations.orEmpty()

        val defaultTypeQualifier =
            if (isHeadTypeConstructor)
                context.defaultTypeQualifiers?.get(containerApplicabilityType)
            else
                defaultQualifiersForType

        val nullabilityInfo = typeQualifierResolver.extractNullability(composedAnnotation).also {
            if (it?.qualifier == NullabilityQualifier.NOT_NULL) {
                attributesCache[this] = composedAnnotation.computeTypeAttributesForJavaType()
            }
        } ?: defaultTypeQualifier?.nullabilityQualifier

        @Suppress("SimplifyBooleanWithConstants")
        return JavaTypeQualifiers(
            nullabilityInfo?.qualifier,
            typeQualifierResolver.extractMutability(composedAnnotation),
            isNotNullTypeParameter = nullabilityInfo?.qualifier == NullabilityQualifier.NOT_NULL && this.isTypeParameterBasedType(),
            isNullabilityQualifierForWarning = nullabilityInfo?.isForWarningOnly == true
        )
    }

    private fun FirTypeRef?.isTypeParameterBasedType() =
        ((this as? FirJavaTypeRef)?.type as? JavaClassifierType)?.classifier is JavaTypeParameter

    private fun computeIndexedQualifiersForOverride(session: FirSession): IndexedJavaTypeQualifiers {
        val indexedFromSupertypes = fromOverridden.map { it.toIndexed(typeQualifierResolver, context) }
        val indexedThisType = current.toIndexed(typeQualifierResolver, context)

        // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
        // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
        // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
        // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
        // Note that `this` is flexible here, so it's equal to it's bounds
        val onlyHeadTypeConstructor = isCovariant && fromOverridden.any { true /*equalTypes(it, this)*/ }

        val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
        val computedResult = Array(treeSize) { index ->
            val (type, defaultQualifiers) = indexedThisType[index]
            val qualifiers = type.extractQualifiersFromAnnotations(index == 0, defaultQualifiers)
            val superQualifiers = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type?.extractQualifiers(session) }
            qualifiers.computeQualifiersForOverride(superQualifiers, index == 0 && isCovariant, index == 0 && isForVarargParameter)
        }

        return IndexedJavaTypeQualifiers(computedResult)
    }

    @Suppress("unused")
    internal open class PartEnhancementResult(
        val type: FirResolvedTypeRef,
        val wereChanges: Boolean,
        val containsFunctionN: Boolean
    )
}
