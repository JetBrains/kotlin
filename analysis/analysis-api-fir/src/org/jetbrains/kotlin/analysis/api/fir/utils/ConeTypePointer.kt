/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.AbbreviatedTypeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeCapturedTypeConstructor
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerConstantOperatorType
import org.jetbrains.kotlin.fir.types.ConeIntegerConstantOperatorTypeImpl
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralConstantType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralConstantTypeImpl
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeRawType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.fir.types.ProjectionKind.*
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.create
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.type

internal fun <T : ConeKotlinType> T.createPointer(builder: KaSymbolByFirBuilder): ConeTypePointer<T> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeDynamicType -> ConeDynamicTypePointer
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullTypePointer(this, builder)
        is ConeIntersectionType -> ConeIntersectionTypePointer(this, builder)
        is ConeRawType -> ConeRawTypePointer(this, builder)
        is ConeFlexibleType -> ConeFlexibleTypePointer(this, builder)
        is ConeCapturedType -> ConeCapturedTypePointer(this, builder)
        is ConeErrorType -> ConeErrorTypePointer(this, builder)
        is ConeClassLikeType -> ConeClassLikeTypePointer(this, builder)
        is ConeTypeParameterType -> ConeTypeParameterTypePointer(this, builder)
        is ConeTypeVariableType -> ConeTypeVariableTypePointer(this, builder)
        is ConeIntegerLiteralConstantType -> ConeIntegerLiteralConstantTypePointer(this, builder)
        is ConeIntegerConstantOperatorType -> ConeIntegerConstantOperatorTypePointer(this)
        else -> ConeNeverRestoringTypePointer
    } as ConeTypePointer<T>
}

/**
 * A pointer for the compiler type representation.
 * Does not hold references to internal compiler abstractions, so it can be restored in a different [KaSession].
 *
 * Meant to be used as a lower level abstraction inside [KaTypePointer]s.
 */
internal interface ConeTypePointer<out T : ConeKotlinType> {
    /**
     * Restores the original type when possible.
     * Returns `null` if restoration is impossible.
     */
    fun restore(session: KaFirSession): T?
}

private class ConeClassLikeTypePointer(coneType: ConeClassLikeType, builder: KaSymbolByFirBuilder) : ConeTypePointer<ConeClassLikeType> {
    private val lookupTag = coneType.lookupTag
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder) }
    private val isNullable = coneType.isNullable
    private val abbreviatedTypePointer = coneType.abbreviatedType?.createPointer(builder)

    override fun restore(session: KaFirSession): ConeClassLikeTypeImpl? {
        val typeArguments = typeArgumentPointers.map { it.restore(session) ?: return null }
        val abbreviatedType = abbreviatedTypePointer?.let { it.restore(session) ?: return null }

        val attributes = buildList {
            if (abbreviatedType != null) {
                add(AbbreviatedTypeAttribute(abbreviatedType))
            }
        }

        return ConeClassLikeTypeImpl(
            lookupTag = lookupTag,
            typeArguments = typeArguments.toTypedArray(),
            isNullable = isNullable,
            attributes = ConeAttributes.create(attributes)
        )
    }
}

private class ConeTypeParameterTypePointer(
    coneType: ConeTypeParameterType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeTypeParameterType> {
    private val typeParameterPointer = builder.classifierBuilder.buildTypeParameterSymbol(coneType.lookupTag.symbol).createPointer()
    private val isNullable = coneType.isNullable

    override fun restore(session: KaFirSession): ConeTypeParameterType? {
        val typeParameterSymbol = typeParameterPointer.restoreSymbol(session) ?: return null

        val lookupTag = ConeTypeParameterLookupTag(typeParameterSymbol.firSymbol)
        return ConeTypeParameterTypeImpl(lookupTag, isNullable)
    }
}

private class ConeTypeVariableTypePointer(
    coneType: ConeTypeVariableType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeTypeVariableType> {
    private val debugName = coneType.typeConstructor.debugName
    private val nullability = coneType.nullability

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = coneType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    override fun restore(session: KaFirSession): ConeTypeVariableType? {
        val typeParameterSymbol = typeParameterSymbolPointer?.let { it.restoreSymbol(session) ?: return null }

        val typeConstructor = ConeTypeVariableTypeConstructor(debugName, typeParameterSymbol?.firSymbol?.toLookupTag())
        return ConeTypeVariableType(nullability, typeConstructor)
    }
}

private class ConeCapturedTypePointer(
    coneType: ConeCapturedType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeCapturedType> {
    private val captureStatus = coneType.captureStatus
    private val lowerTypePointer = coneType.lowerType?.createPointer(builder)
    private val nullability = coneType.nullability
    private val coneProjectionPointer = ConeTypeProjectionPointer(coneType.constructor.projection, builder)
    private val constructorSupertypePointers = coneType.constructor.supertypes?.map { it.createPointer(builder) }
    private val isProjectionNotNull = coneType.isProjectionNotNull

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = coneType.constructor.typeParameterMarker as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    override fun restore(session: KaFirSession): ConeCapturedType? {
        val lowerType = lowerTypePointer?.let { it.restore(session) ?: return null }

        val constructorProjection = coneProjectionPointer.restore(session) ?: return null
        val constructorSupertypes = constructorSupertypePointers?.let { it.restore(session) ?: return null }

        val constructorTypeParameterMarker = if (typeParameterSymbolPointer != null) {
            val typeParameterSymbol = with(session) { typeParameterSymbolPointer.restoreSymbol() } ?: return null
            typeParameterSymbol.firSymbol.toLookupTag()
        } else {
            null
        }

        val typeConstructor = ConeCapturedTypeConstructor(
            constructorProjection,
            constructorSupertypes,
            constructorTypeParameterMarker
        )

        return ConeCapturedType(
            captureStatus,
            lowerType,
            nullability,
            typeConstructor,
            isProjectionNotNull = isProjectionNotNull
        )
    }
}

private class ConeIntersectionTypePointer(
    coneType: ConeIntersectionType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeIntersectionType> {
    private val intersectedTypePointers = coneType.intersectedTypes.map { it.createPointer(builder) }
    private val upperBoundForApproximationPointer = coneType.upperBoundForApproximation?.createPointer(builder)

    override fun restore(session: KaFirSession): ConeIntersectionType? {
        val intersectedTypes = intersectedTypePointers.restore(session) ?: return null
        val upperBoundForApproximation = upperBoundForApproximationPointer?.let { it.restore(session) ?: return null }

        return ConeIntersectionType(intersectedTypes, upperBoundForApproximation)
    }
}

private abstract class AbstractConeFlexibleTypePointer<T : ConeFlexibleType>(
    coneType: ConeFlexibleType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<T> {
    private val lowerBoundPointer = coneType.lowerBound.createPointer(builder)
    private val upperBoundPointer = coneType.upperBound.createPointer(builder)

    override fun restore(session: KaFirSession): T? {
        val lowerBound = lowerBoundPointer.restore(session) ?: return null
        val upperBound = upperBoundPointer.restore(session) ?: return null

        return restore(lowerBound, upperBound)
    }

    abstract fun restore(lowerBound: ConeSimpleKotlinType, upperBound: ConeSimpleKotlinType): T
}

private class ConeFlexibleTypePointer(
    coneType: ConeFlexibleType,
    builder: KaSymbolByFirBuilder,
) : AbstractConeFlexibleTypePointer<ConeFlexibleType>(coneType, builder) {
    override fun restore(lowerBound: ConeSimpleKotlinType, upperBound: ConeSimpleKotlinType): ConeFlexibleType {
        return ConeFlexibleType(lowerBound, upperBound)
    }
}

private class ConeRawTypePointer(
    coneType: ConeFlexibleType,
    builder: KaSymbolByFirBuilder,
) : AbstractConeFlexibleTypePointer<ConeFlexibleType>(coneType, builder) {
    override fun restore(lowerBound: ConeSimpleKotlinType, upperBound: ConeSimpleKotlinType): ConeFlexibleType {
        return ConeRawType.create(lowerBound, upperBound)
    }
}

private class ConeDefinitelyNotNullTypePointer(
    coneType: ConeDefinitelyNotNullType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeDefinitelyNotNullType> {
    private val originalTypePointer = coneType.original.createPointer(builder)

    override fun restore(session: KaFirSession): ConeDefinitelyNotNullType? {
        val originalType = originalTypePointer.restore(session) ?: return null
        return ConeDefinitelyNotNullType(originalType)
    }
}

private class ConeIntegerLiteralConstantTypePointer(
    coneType: ConeIntegerLiteralConstantType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeIntegerLiteralConstantType> {
    private val value = coneType.value
    private val possibleTypePointers = coneType.possibleTypes.map { it.createPointer(builder) }
    private val isUnsigned = coneType.isUnsigned
    private val nullability = coneType.nullability

    override fun restore(session: KaFirSession): ConeIntegerLiteralConstantType? {
        val possibleTypes = possibleTypePointers.restore(session) ?: return null
        return ConeIntegerLiteralConstantTypeImpl(value, possibleTypes, isUnsigned, nullability)
    }
}

private class ConeIntegerConstantOperatorTypePointer(
    coneType: ConeIntegerConstantOperatorType
) : ConeTypePointer<ConeIntegerConstantOperatorType> {
    private val isUnsigned = coneType.isUnsigned
    private val nullability = coneType.nullability

    override fun restore(session: KaFirSession): ConeIntegerConstantOperatorType? {
        return ConeIntegerConstantOperatorTypeImpl(isUnsigned, nullability)
    }
}

private class ConeErrorTypePointer(coneType: ConeErrorType, builder: KaSymbolByFirBuilder) : ConeTypePointer<ConeErrorType> {
    @Suppress("SpellCheckingInspection")
    private val isUninferredParameter = coneType.isUninferredParameter

    private val coneDiagnosticPointer = ConeDiagnosticPointer.create(coneType.diagnostic, builder)
    private val delegatedTypePointer = coneType.delegatedType?.createPointer(builder)
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder) }

    override fun restore(session: KaFirSession): ConeErrorType? {
        val coneDiagnostic = coneDiagnosticPointer.restore(session) ?: return null
        val delegatedConeType = delegatedTypePointer?.let { it.restore(session) ?: return null }
        val typeArguments = typeArgumentPointers.restore(session) ?: return null

        return ConeErrorType(
            diagnostic = coneDiagnostic,
            isUninferredParameter = isUninferredParameter,
            delegatedType = delegatedConeType,
            typeArguments = typeArguments.toTypedArray()
        )
    }
}

private object ConeDynamicTypePointer : ConeTypePointer<ConeDynamicType> {
    override fun restore(session: KaFirSession): ConeDynamicType? {
        return ConeDynamicType.create(session.firSession)
    }
}

private object ConeNeverRestoringTypePointer : ConeTypePointer<ConeKotlinType> {
    override fun restore(session: KaFirSession): ConeKotlinType? {
        return null
    }
}

internal class ConeTypeProjectionPointer(projection: ConeTypeProjection, builder: KaSymbolByFirBuilder) {
    private val kind = projection.kind
    private val typePointer: ConeTypePointer<*>? = projection.type?.createPointer(builder)

    fun restore(session: KaFirSession): ConeTypeProjection? {
        if (kind == STAR) {
            return ConeStarProjection
        }

        requireNotNull(typePointer)
        val type = typePointer.restore(session) ?: return null

        return when (kind) {
            IN -> ConeKotlinTypeProjectionIn(type)
            OUT -> ConeKotlinTypeProjectionOut(type)
            INVARIANT -> type
            STAR -> error("Should be handled above")
        }
    }
}

/**
 * Restores all type pointers in a list.
 * Returns `null` if at least one pointer cannot be restored.
 */
@JvmName("restoreTypes")
private fun <T : ConeKotlinType> List<ConeTypePointer<T>>.restore(session: KaFirSession): List<T>? {
    return restoreAll { it.restore(session) }
}

/**
 * Restores all type projection pointers in a list.
 * Returns `null` if at least one pointer cannot be restored.
 */
@JvmName("restoreTypeProjections")
private fun List<ConeTypeProjectionPointer>.restore(session: KaFirSession): List<ConeTypeProjection>? {
    return restoreAll { it.restore(session) }
}

private inline fun <T : Any, R : Any> List<T>.restoreAll(factory: (T) -> R?): List<R>? {
    return buildList(size) {
        for (pointer in this@restoreAll) {
            val item = factory(pointer) ?: return null
            add(item)
        }
    }
}