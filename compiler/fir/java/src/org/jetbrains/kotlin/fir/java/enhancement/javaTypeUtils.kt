/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.resolvedFqName
import org.jetbrains.kotlin.fir.java.createTypeParameterSymbol
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeWithNullability
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.toTypeProjection
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.AnnotationDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun FirJavaTypeRef.enhance(
    qualifiers: (Int) -> JavaTypeQualifiers
): FirResolvedTypeRef {
    return type.enhancePossiblyFlexible(session, annotations, qualifiers, 0)
}

// The index in the lambda is the position of the type component:
// Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
// which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
// For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
private fun JavaType.enhancePossiblyFlexible(
    session: FirSession,
    annotations: List<FirAnnotationCall>,
    qualifiers: (Int) -> JavaTypeQualifiers,
    index: Int
): FirResolvedTypeRef {
    val type = this
    val arguments = typeArguments()
    return when (type) {
        is JavaClassifierType -> {
            val lowerResult = type.enhanceInflexibleType(
                session, annotations, arguments, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index
            )
            val upperResult = type.enhanceInflexibleType(
                session, annotations, arguments, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index
            )

            FirResolvedTypeRefImpl(
                session, psi = null,
                type = ConeFlexibleType(lowerResult, upperResult),
                isMarkedNullable = false, annotations = annotations
            )
        }
        else -> {
            val enhanced = type.toNotNullConeKotlinType(session)
            FirResolvedTypeRefImpl(session, psi = null, type = enhanced, isMarkedNullable = false, annotations = annotations)
        }
    }
}

private fun JavaType.subtreeSize(): Int {
    if (this !is JavaClassifierType) return 1
    return 1 + typeArguments.sumBy { it.subtreeSize() }
}

private val KOTLIN_COLLECTIONS = FqName("kotlin.collections")

private val KOTLIN_COLLECTIONS_PREFIX_LENGTH = KOTLIN_COLLECTIONS.asString().length + 1

private fun ClassId.readOnlyToMutable(): ClassId? {
    val mutableFqName = JavaToKotlinClassMap.readOnlyToMutable(asSingleFqName().toUnsafe())
    return mutableFqName?.let {
        ClassId(KOTLIN_COLLECTIONS, FqName(it.asString().substring(KOTLIN_COLLECTIONS_PREFIX_LENGTH)), false)
    }
}

private fun ClassId.mutableToReadOnly(): ClassId? {
    val readOnlyFqName = JavaToKotlinClassMap.mutableToReadOnly(asSingleFqName().toUnsafe())
    return readOnlyFqName?.let {
        ClassId(KOTLIN_COLLECTIONS, FqName(it.asString().substring(KOTLIN_COLLECTIONS_PREFIX_LENGTH)), false)
    }
}

private fun JavaClassifierType.enhanceInflexibleType(
    session: FirSession,
    annotations: List<FirAnnotationCall>,
    arguments: List<JavaType>,
    position: TypeComponentPosition,
    qualifiers: (Int) -> JavaTypeQualifiers,
    index: Int
): ConeLookupTagBasedType {
    val originalSymbol = when (val classifier = classifier) {
        is JavaClass -> {
            val classId = classifier.classId!!
            var mappedId = JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName())
            if (mappedId != null) {
                if (position == TypeComponentPosition.FLEXIBLE_LOWER) {
                    mappedId = mappedId.readOnlyToMutable() ?: mappedId
                }
            }
            session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(mappedId ?: classId)!!
        }
        is JavaTypeParameter -> createTypeParameterSymbol(session, classifier.name)
        else -> return toNotNullConeKotlinType(session)
    }

    val effectiveQualifiers = qualifiers(index)
    val (enhancedSymbol, mutabilityChanged) = originalSymbol.enhanceMutability(effectiveQualifiers, position)

    var globalArgIndex = index + 1
    var wereChanges = mutabilityChanged
    val enhancedArguments = arguments.mapIndexed { localArgIndex, arg ->
        if (arg is JavaWildcardType) {
            globalArgIndex++
            ConeStarProjection
            // TODO: (?) TypeUtils.makeStarProjection(enhancedClassifier.typeConstructor.parameters[localArgIndex])
        } else {
            val argEnhancedTypeRef = arg.enhancePossiblyFlexible(session, annotations, qualifiers, globalArgIndex)
            wereChanges = wereChanges || argEnhancedTypeRef !== arg
            globalArgIndex += arg.subtreeSize()
            argEnhancedTypeRef.type.type.toTypeProjection(Variance.INVARIANT)
        }
    }

    val (enhancedNullability, _, nullabilityChanged) = getEnhancedNullability(effectiveQualifiers, position)
    wereChanges = wereChanges || nullabilityChanged

    if (!wereChanges) return toConeKotlinTypeWithNullability(
        session, isNullable = position == TypeComponentPosition.FLEXIBLE_UPPER
    )

    val enhancedType = enhancedSymbol.constructType(enhancedArguments.toTypedArray(), enhancedNullability)

    // TODO: why all of these is needed
//    val enhancement = if (effectiveQualifiers.isNotNullTypeParameter) NotNullTypeParameter(enhancedType) else enhancedType
//    val nullabilityForWarning = nullabilityChanged && effectiveQualifiers.isNullabilityQualifierForWarning
//    val result = if (nullabilityForWarning) wrapEnhancement(enhancement) else enhancement

    return enhancedType
}

private fun getEnhancedNullability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): EnhanceDetailsResult<Boolean> {
    if (!position.shouldEnhance()) return false.noChange()

    return when (qualifiers.nullability) {
        NullabilityQualifier.NULLABLE -> true.enhancedNullability()
        NullabilityQualifier.NOT_NULL -> false.enhancedNullability()
        else -> false.noChange()
    }
}

private data class EnhanceDetailsResult<out T>(
    val result: T,
    val mutabilityChanged: Boolean = false,
    val nullabilityChanged: Boolean = false
)

private fun <T> T.noChange() = EnhanceDetailsResult(this)
private fun <T> T.enhancedNullability() = EnhanceDetailsResult(this, nullabilityChanged = true)
private fun <T> T.enhancedMutability() = EnhanceDetailsResult(this, mutabilityChanged = true)

private fun ConeClassifierSymbol.enhanceMutability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): EnhanceDetailsResult<ConeClassifierSymbol> {
    if (!position.shouldEnhance()) return this.noChange()
    if (this !is FirClassSymbol) return this.noChange() // mutability is not applicable for type parameters

    when (qualifiers.mutability) {
        MutabilityQualifier.READ_ONLY -> {
            val readOnlyId = classId.mutableToReadOnly()
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && readOnlyId != null) {
                return FirClassSymbol(readOnlyId).enhancedMutability()
            }
        }
        MutabilityQualifier.MUTABLE -> {
            val mutableId = classId.readOnlyToMutable()
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mutableId != null) {
                return FirClassSymbol(mutableId).enhancedMutability()
            }
        }
    }

    return this.noChange()
}


internal data class TypeAndDefaultQualifiers(
    val type: FirTypeRef,
    val defaultQualifiers: JavaTypeQualifiers?
)

internal fun FirTypeRef.typeArguments(): List<FirTypeProjection> =
    (this as? FirUserTypeRef)?.qualifier?.lastOrNull()?.typeArguments.orEmpty()

internal fun JavaType.typeArguments(): List<JavaType> = (this as? JavaClassifierType)?.typeArguments.orEmpty()

fun FirValueParameter.getDefaultValueFromAnnotation(): AnnotationDefaultValue? {
    annotations.find { it.resolvedFqName == JvmAnnotationNames.DEFAULT_VALUE_FQ_NAME }
        ?.arguments?.firstOrNull()
        ?.safeAs<FirConstExpression<*>>()?.value?.safeAs<String>()
        ?.let { return StringDefaultValue(it) }

    if (annotations.any { it.resolvedFqName == JvmAnnotationNames.DEFAULT_NULL_FQ_NAME }) {
        return NullDefaultValue
    }

    return null
}

