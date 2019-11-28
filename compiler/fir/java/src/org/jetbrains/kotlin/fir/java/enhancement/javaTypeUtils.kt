/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.toConeProjection
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_NULL_FQ_NAME
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_VALUE_FQ_NAME
import org.jetbrains.kotlin.load.java.descriptors.AnnotationDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.extractRadix

internal class IndexedJavaTypeQualifiers(private val data: Array<JavaTypeQualifiers>) {
    constructor(size: Int, compute: (Int) -> JavaTypeQualifiers) : this(Array(size) { compute(it) })

    operator fun invoke(index: Int) = data.getOrElse(index) { JavaTypeQualifiers.NONE }

    val size: Int get() = data.size
}

internal fun FirJavaTypeRef.enhance(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    qualifiers: IndexedJavaTypeQualifiers
): FirResolvedTypeRef {
    return type.enhancePossiblyFlexible(session, javaTypeParameterStack, annotations, qualifiers, 0)
}

// The index in the lambda is the position of the type component:
// Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
// which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
// For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
private fun JavaType?.enhancePossiblyFlexible(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    annotations: List<FirAnnotationCall>,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int
): FirResolvedTypeRef {
    val type = this
    val arguments = this?.typeArguments().orEmpty()
    val enhanced = when (type) {
        is JavaClassifierType -> {
            val lowerResult = type.enhanceInflexibleType(
                session, javaTypeParameterStack, annotations, arguments, TypeComponentPosition.FLEXIBLE_LOWER, qualifiers, index
            )
            val upperResult = type.enhanceInflexibleType(
                session, javaTypeParameterStack, annotations, arguments, TypeComponentPosition.FLEXIBLE_UPPER, qualifiers, index
            )

            when {
                type.isRaw -> ConeRawType(lowerResult, upperResult)
                else -> coneFlexibleOrSimpleType(
                    session, lowerResult, upperResult, isNotNullTypeParameter = qualifiers(index).isNotNullTypeParameter
                )
            }
        }
        is JavaArrayType -> {
            val baseEnhanced = type.toNotNullConeKotlinType(session, javaTypeParameterStack)

            val upperBound = if (baseEnhanced.typeArguments.isNotEmpty()) {
                val typeArgument = baseEnhanced.typeArguments.first() as ConeKotlinType
                baseEnhanced.withArguments(arrayOf(ConeKotlinTypeProjectionOut(typeArgument)))
            } else {
                baseEnhanced
            }
            coneFlexibleOrSimpleType(
                session, baseEnhanced,
                upperBound.withNullability(ConeNullability.NULLABLE),
                isNotNullTypeParameter = false
            )
        }
        else -> {
            type.toNotNullConeKotlinType(session, javaTypeParameterStack)
        }
    }

    return FirResolvedTypeRefImpl(source = null, type = enhanced).apply {
        this.annotations += annotations
    }
}

private fun JavaType?.subtreeSize(): Int {
    if (this !is JavaClassifierType) return 1
    return 1 + typeArguments.sumBy { it?.subtreeSize() ?: 0 }
}

private fun coneFlexibleOrSimpleType(
    session: FirSession,
    lowerBound: ConeLookupTagBasedType,
    upperBound: ConeLookupTagBasedType,
    isNotNullTypeParameter: Boolean
): ConeKotlinType {
    if (AbstractStrictEqualityTypeChecker.strictEqualTypes(session.typeContext, lowerBound, upperBound)) {
        val lookupTag = lowerBound.lookupTag
        if (isNotNullTypeParameter && lookupTag is ConeTypeParameterLookupTag && !lowerBound.isMarkedNullable) {
            // TODO: we need enhancement for type parameter bounds for this code to work properly
            // At this moment, this condition is always true
            if (lookupTag.typeParameterSymbol.fir.bounds.any {
                    val type = (it as FirResolvedTypeRef).type
                    type is ConeTypeParameterType || type.isNullable
                }
            ) {
                return ConeDefinitelyNotNullType.create(lowerBound)
            }
        }
        return lowerBound
    }
    return ConeFlexibleType(lowerBound, upperBound)
}

private val KOTLIN_COLLECTIONS = FqName("kotlin.collections")

private val KOTLIN_COLLECTIONS_PREFIX_LENGTH = KOTLIN_COLLECTIONS.asString().length + 1

internal fun ClassId.readOnlyToMutable(): ClassId? {
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



// Definition:
// ErasedUpperBound(T : G<t>) = G<*> // UpperBound(T) is a type G<t> with arguments
// ErasedUpperBound(T : A) = A // UpperBound(T) is a type A without arguments
// ErasedUpperBound(T : F) = UpperBound(F) // UB(T) is another type parameter F
private fun FirTypeParameter.getErasedUpperBound(
    // Calculation of `potentiallyRecursiveTypeParameter.upperBounds` may recursively depend on `this.getErasedUpperBound`
    // E.g. `class A<T extends A, F extends A>`
    // To prevent recursive calls return defaultValue() instead
    potentiallyRecursiveTypeParameter: FirTypeParameter? = null,
    defaultValue: (() -> ConeKotlinType) = { ConeKotlinErrorType("Can't compute erased upper bound of type parameter `$this`") }
): ConeKotlinType {
    if (this === potentiallyRecursiveTypeParameter) return defaultValue()

    val firstUpperBound = this.bounds.first().coneTypeUnsafe<ConeKotlinType>()

    val firstUpperBoundClassifier = firstUpperBound
    if (firstUpperBoundClassifier is ConeClassLikeType) {
        return firstUpperBound.withArguments(firstUpperBound.typeArguments.map { ConeStarProjection }.toTypedArray())
    }

    val alreadyVisited = mutableSetOf(potentiallyRecursiveTypeParameter, this)
    var current = (firstUpperBound as ConeTypeParameterType).lookupTag.typeParameterSymbol.fir

    while (current !in alreadyVisited) {
        alreadyVisited += current

        val nextUpperBound = current.bounds.first().coneTypeUnsafe<ConeKotlinType>()
        if (nextUpperBound is ConeClassLikeType) {
            return nextUpperBound.withArguments(nextUpperBound.typeArguments.map { ConeStarProjection }.toTypedArray())
        }

        current = (nextUpperBound as ConeTypeParameterType).lookupTag.typeParameterSymbol.fir
    }

    return defaultValue()
}


fun computeProjection(
    session: FirSession,
    parameter: FirTypeParameter,
    attr: TypeComponentPosition,
    erasedUpperBound: ConeKotlinType = parameter.getErasedUpperBound()
) = when (attr) {
    // Raw(List<T>) => (List<Any?>..List<*>)
    // Raw(Enum<T>) => (Enum<Enum<*>>..Enum<out Enum<*>>)
    // In the last case upper bound is equal to star projection `Enum<*>`,
    // but we want to keep matching tree structure of flexible bounds (at least they should have the same size)
    TypeComponentPosition.FLEXIBLE_LOWER -> {
        // T : String -> String
        // in T : String -> String
        // T : Enum<T> -> Enum<*>
        erasedUpperBound
    }
    TypeComponentPosition.FLEXIBLE_UPPER, TypeComponentPosition.INFLEXIBLE -> {
        if (!parameter.variance.allowsOutPosition)
        // in T -> Comparable<Nothing>
            session.builtinTypes.nothingType.type
        else if (erasedUpperBound is ConeClassLikeType &&
            erasedUpperBound.lookupTag.toSymbol(session)!!.firUnsafe<FirRegularClass>().typeParameters.isNotEmpty())
        // T : Enum<E> -> out Enum<*>
            ConeKotlinTypeProjectionOut(erasedUpperBound)
        else
        // T : String -> *
            ConeStarProjection
    }
}



private fun JavaClassifierType.enhanceInflexibleType(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    annotations: List<FirAnnotationCall>,
    arguments: List<JavaType?>,
    position: TypeComponentPosition,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int
): ConeLookupTagBasedType {
    val classifier = classifier
    val originalTag = when (classifier) {
        is JavaClass -> {
            val classId = classifier.classId!!
            var mappedId = JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName())
            if (mappedId != null) {
                if (position == TypeComponentPosition.FLEXIBLE_LOWER) {
                    mappedId = mappedId.readOnlyToMutable() ?: mappedId
                }
            }
            val kotlinClassId = mappedId ?: classId
            ConeClassLikeLookupTagImpl(kotlinClassId)
        }
        is JavaTypeParameter -> javaTypeParameterStack[classifier].toLookupTag()
        else -> return toNotNullConeKotlinType(session, javaTypeParameterStack)
    }

    val effectiveQualifiers = qualifiers(index)
    val enhancedTag = originalTag.enhanceMutability(effectiveQualifiers, position)

    val enhancedArguments = if (isRaw) {
        val firClassifier = originalTag.toSymbol(session)!!.firUnsafe<FirRegularClass>()
        firClassifier.typeParameters.map {
            val fir = it
            val erasedUpperBound = fir.getErasedUpperBound {
                firClassifier.defaultType().withArguments(firClassifier.typeParameters.map { ConeStarProjection }.toTypedArray())
            }
            computeProjection(session, fir, position, erasedUpperBound)
        }
    } else {
        var globalArgIndex = index + 1
        arguments.mapIndexed { localArgIndex, arg ->
            if (arg is JavaWildcardType) {
                globalArgIndex++
                arg.toConeProjection(
                    session,
                    javaTypeParameterStack,
                    ((originalTag as? FirBasedSymbol<*>)?.fir as? FirCallableMemberDeclaration<*>)?.typeParameters?.getOrNull(localArgIndex)
                )
            } else {
                val argEnhancedTypeRef =
                    arg.enhancePossiblyFlexible(session, javaTypeParameterStack, annotations, qualifiers, globalArgIndex)
                globalArgIndex += arg.subtreeSize()

                argEnhancedTypeRef.type.type.toTypeProjection(Variance.INVARIANT)
            }
        }
    }

    val enhancedNullability = getEnhancedNullability(effectiveQualifiers, position)

    val enhancedType = enhancedTag.constructType(enhancedArguments.toTypedArray(), enhancedNullability)

    // TODO: why all of these is needed
//    val enhancement = if (effectiveQualifiers.isNotNullTypeParameter) NotNullTypeParameter(enhancedType) else enhancedType
//    val nullabilityForWarning = nullabilityChanged && effectiveQualifiers.isNullabilityQualifierForWarning
//    val result = if (nullabilityForWarning) wrapEnhancement(enhancement) else enhancement

    return enhancedType
}

private fun getEnhancedNullability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): Boolean {
    if (!position.shouldEnhance()) return position == TypeComponentPosition.FLEXIBLE_UPPER

    return when (qualifiers.nullability) {
        NullabilityQualifier.NULLABLE -> true
        NullabilityQualifier.NOT_NULL -> false
        else -> position == TypeComponentPosition.FLEXIBLE_UPPER
    }
}

private fun ConeClassifierLookupTag.enhanceMutability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): ConeClassifierLookupTag {
    if (!position.shouldEnhance()) return this
    if (this !is ConeClassLikeLookupTag) return this // mutability is not applicable for type parameters

    when (qualifiers.mutability) {
        MutabilityQualifier.READ_ONLY -> {
            val readOnlyId = classId.mutableToReadOnly()
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && readOnlyId != null) {
                return ConeClassLikeLookupTagImpl(readOnlyId)
            }
        }
        MutabilityQualifier.MUTABLE -> {
            val mutableId = classId.readOnlyToMutable()
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mutableId != null) {
                return ConeClassLikeLookupTagImpl(mutableId)
            }
        }
    }

    return this
}


internal data class TypeAndDefaultQualifiers(
    val type: FirTypeRef?, // null denotes '*' here
    val defaultQualifiers: JavaTypeQualifiers?
)

internal fun FirTypeRef.typeArguments(): List<FirTypeProjection> =
    (this as? FirUserTypeRef)?.qualifier?.lastOrNull()?.typeArguments.orEmpty()

internal fun JavaType.typeArguments(): List<JavaType?> = (this as? JavaClassifierType)?.typeArguments.orEmpty()

internal fun ConeKotlinType.lexicalCastFrom(session: FirSession, value: String): FirExpression? {
    val lookupTagBasedType = when (this) {
        is ConeLookupTagBasedType -> this
        is ConeFlexibleType -> return lowerBound.lexicalCastFrom(session, value)
        else -> return null
    }
    val lookupTag = lookupTagBasedType.lookupTag
    val firElement = lookupTag.toSymbol(session)?.fir
    if (firElement is FirRegularClass && firElement.classKind == ClassKind.ENUM_CLASS) {
        val name = Name.identifier(value)
        val firEnumEntry = firElement.collectEnumEntries().find { it.callableId.callableName == name }

        return if (firEnumEntry != null) FirQualifiedAccessExpressionImpl(null).apply {
            calleeReference = FirResolvedNamedReferenceImpl(
                null, name, firEnumEntry
            )
        } else if (firElement is FirJavaClass) {
            val firStaticProperty = firElement.declarations.filterIsInstance<FirJavaField>().find {
                it.isStatic && it.modality == Modality.FINAL && it.name == name
            }
            if (firStaticProperty != null) {
                FirQualifiedAccessExpressionImpl(null).apply {
                    calleeReference = FirResolvedNamedReferenceImpl(
                        null, name, firStaticProperty.symbol as FirCallableSymbol<*>
                    )
                }
            } else null
        } else null
    }

    if (lookupTag !is ConeClassLikeLookupTag) return null
    val classId = lookupTag.classId
    if (classId.packageFqName != FqName("kotlin")) return null

    val (number, radix) = extractRadix(value)
    return when (classId.relativeClassName.asString()) {
        "Boolean" -> FirConstExpressionImpl(null, FirConstKind.Boolean, value.toBoolean())
        "Char" -> FirConstExpressionImpl(null, FirConstKind.Char, value.singleOrNull() ?: return null)
        "Byte" -> FirConstExpressionImpl(null, FirConstKind.Byte, number.toByteOrNull(radix) ?: return null)
        "Short" -> FirConstExpressionImpl(null, FirConstKind.Short, number.toShortOrNull(radix) ?: return null)
        "Int" -> FirConstExpressionImpl(null, FirConstKind.Int, number.toIntOrNull(radix) ?: return null)
        "Long" -> FirConstExpressionImpl(null, FirConstKind.Long, number.toLongOrNull(radix) ?: return null)
        "Float" -> FirConstExpressionImpl(null, FirConstKind.Float, value.toFloatOrNull() ?: return null)
        "Double" -> FirConstExpressionImpl(null, FirConstKind.Double, value.toDoubleOrNull() ?: return null)
        "String" -> FirConstExpressionImpl(null, FirConstKind.String, value)
        else -> null
    }
}

internal fun FirValueParameter.getDefaultValueFromAnnotation(): AnnotationDefaultValue? {
    annotations.find { it.classId == DEFAULT_VALUE_ID }
        ?.arguments?.firstOrNull()
        ?.safeAs<FirConstExpression<*>>()?.value?.safeAs<String>()
        ?.let { return StringDefaultValue(it) }

    if (annotations.any { it.classId == DEFAULT_NULL_ID }) {
        return NullDefaultValue
    }

    return null
}

private val DEFAULT_VALUE_ID = ClassId.topLevel(DEFAULT_VALUE_FQ_NAME)
private val DEFAULT_NULL_ID = ClassId.topLevel(DEFAULT_NULL_FQ_NAME)

