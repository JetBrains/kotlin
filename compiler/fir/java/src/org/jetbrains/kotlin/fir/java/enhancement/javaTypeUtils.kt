/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.fir.java.createTypeParameterSymbol
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.toConeProjection
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeProjection
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
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

internal fun FirJavaTypeRef.enhance(session: FirSession, qualifiers: IndexedJavaTypeQualifiers): FirResolvedTypeRef {
    return type.enhancePossiblyFlexible(session, annotations, qualifiers, 0)
}

// The index in the lambda is the position of the type component:
// Example: for `A<B, C<D, E>>`, indices go as follows: `0 - A<...>, 1 - B, 2 - C<D, E>, 3 - D, 4 - E`,
// which corresponds to the left-to-right breadth-first walk of the tree representation of the type.
// For flexible types, both bounds are indexed in the same way: `(A<B>..C<D>)` gives `0 - (A<B>..C<D>), 1 - B and D`.
private fun JavaType?.enhancePossiblyFlexible(
    session: FirSession,
    annotations: List<FirAnnotationCall>,
    qualifiers: IndexedJavaTypeQualifiers,
    index: Int
): FirResolvedTypeRef {
    val type = this
    val arguments = this?.typeArguments().orEmpty()
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
                type = coneFlexibleOrSimpleType(session, lowerResult, upperResult),
                isMarkedNullable = false, annotations = annotations
            )
        }
        else -> {
            val enhanced = type.toNotNullConeKotlinType(session)
            FirResolvedTypeRefImpl(session, psi = null, type = enhanced, isMarkedNullable = false, annotations = annotations)
        }
    }
}

private fun JavaType?.subtreeSize(): Int {
    if (this !is JavaClassifierType) return 1
    return 1 + typeArguments.sumBy { it?.subtreeSize() ?: 0 }
}

private fun coneFlexibleOrSimpleType(
    session: FirSession,
    lowerBound: ConeLookupTagBasedType,
    upperBound: ConeLookupTagBasedType
): ConeKotlinType {
    if (AbstractStrictEqualityTypeChecker.strictEqualTypes(session.typeContext, lowerBound, upperBound)) {
        return lowerBound
    }
    return ConeFlexibleType(lowerBound, upperBound)
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
    arguments: List<JavaType?>,
    position: TypeComponentPosition,
    qualifiers: IndexedJavaTypeQualifiers,
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
            val kotlinClassId = mappedId ?: classId
            session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(kotlinClassId)
                ?: return ConeClassErrorType("Cannot find class-like symbol for $kotlinClassId during enhancement")
        }
        is JavaTypeParameter -> createTypeParameterSymbol(session, classifier.name)
        else -> return toNotNullConeKotlinType(session)
    }

    val effectiveQualifiers = qualifiers(index)
    val enhancedSymbol = originalSymbol.enhanceMutability(effectiveQualifiers, position)

    var globalArgIndex = index + 1
    val enhancedArguments = arguments.mapIndexed { localArgIndex, arg ->
        if (arg is JavaWildcardType) {
            globalArgIndex++
            arg.toConeProjection(
                session,
                ((originalSymbol as? FirBasedSymbol<*>)?.fir as? FirCallableMember)?.typeParameters?.getOrNull(localArgIndex)
            )
        } else {
            val argEnhancedTypeRef = arg.enhancePossiblyFlexible(session, annotations, qualifiers, globalArgIndex)
            globalArgIndex += arg.subtreeSize()
            argEnhancedTypeRef.type.type.toTypeProjection(Variance.INVARIANT)
        }
    }

    val enhancedNullability = getEnhancedNullability(effectiveQualifiers, position)

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
): Boolean {
    if (!position.shouldEnhance()) return position == TypeComponentPosition.FLEXIBLE_UPPER

    return when (qualifiers.nullability) {
        NullabilityQualifier.NULLABLE -> true
        NullabilityQualifier.NOT_NULL -> false
        else -> position == TypeComponentPosition.FLEXIBLE_UPPER
    }
}

private fun ConeClassifierSymbol.enhanceMutability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition
): ConeClassifierSymbol {
    if (!position.shouldEnhance()) return this
    if (this !is FirClassSymbol) return this // mutability is not applicable for type parameters

    when (qualifiers.mutability) {
        MutabilityQualifier.READ_ONLY -> {
            val readOnlyId = classId.mutableToReadOnly()
            if (position == TypeComponentPosition.FLEXIBLE_LOWER && readOnlyId != null) {
                return FirClassSymbol(readOnlyId)
            }
        }
        MutabilityQualifier.MUTABLE -> {
            val mutableId = classId.readOnlyToMutable()
            if (position == TypeComponentPosition.FLEXIBLE_UPPER && mutableId != null) {
                return FirClassSymbol(mutableId)
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
    val firElement = (lookupTag.toSymbol(session) as? FirBasedSymbol<*>)?.fir
    if (firElement is FirRegularClass && firElement.classKind == ClassKind.ENUM_CLASS) {
        val name = Name.identifier(value)
        val firEnumEntry = firElement.declarations.filterIsInstance<FirEnumEntry>().find { it.name == name }

        return if (firEnumEntry != null) FirQualifiedAccessExpressionImpl(session, null).apply {
            calleeReference = FirSimpleNamedReference(
                session, null, name // TODO: , firEnumEntry.symbol
            )
        } else if (firElement is FirJavaClass) {
            val firStaticProperty = firElement.declarations.filterIsInstance<FirJavaField>().find {
                it.isStatic && it.modality == Modality.FINAL && it.name == name
            }
            if (firStaticProperty != null) {
                FirQualifiedAccessExpressionImpl(session, null).apply {
                    calleeReference = FirResolvedCallableReferenceImpl(
                        session, null, name, firStaticProperty.symbol as ConeCallableSymbol
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
        "Boolean" -> FirConstExpressionImpl(session, null, IrConstKind.Boolean, value.toBoolean())
        "Char" -> FirConstExpressionImpl(session, null, IrConstKind.Char, value.singleOrNull() ?: return null)
        "Byte" -> FirConstExpressionImpl(session, null, IrConstKind.Byte, number.toByteOrNull(radix) ?: return null)
        "Short" -> FirConstExpressionImpl(session, null, IrConstKind.Short, number.toShortOrNull(radix) ?: return null)
        "Int" -> FirConstExpressionImpl(session, null, IrConstKind.Int, number.toIntOrNull(radix) ?: return null)
        "Long" -> FirConstExpressionImpl(session, null, IrConstKind.Long, number.toLongOrNull(radix) ?: return null)
        "Float" -> FirConstExpressionImpl(session, null, IrConstKind.Float, value.toFloatOrNull() ?: return null)
        "Double" -> FirConstExpressionImpl(session, null, IrConstKind.Double, value.toDoubleOrNull() ?: return null)
        "String" -> FirConstExpressionImpl(session, null, IrConstKind.String, value)
        else -> null
    }
}

internal fun FirValueParameter.getDefaultValueFromAnnotation(): AnnotationDefaultValue? {
    annotations.find { it.resolvedFqName == JvmAnnotationNames.DEFAULT_VALUE_FQ_NAME }
        ?.arguments?.firstOrNull()
        ?.safeAs<FirConstExpression<*>>()?.value?.safeAs<String>()
        ?.let { return StringDefaultValue(it) }

    if (annotations.any { it.resolvedFqName == JvmAnnotationNames.DEFAULT_NULL_FQ_NAME }) {
        return NullDefaultValue
    }

    return null
}

