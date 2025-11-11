/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ProjectionKind.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * Unified recursion guard for both pointer creation and restoration.
 * Tracks visited items to detect and break cycles in recursive type structures.
 *
 * For recursive pointer creation, it's enough to create delegating pointers that reference
 * a wrapper, breaking the cycle without requiring special handling.
 *
 * For pointer *restoration*, the situation is more complex: you cannot create a fully correct
 * recursive [ConeKotlinType] in one go. Instead, you must first create an incomplete type object
 * and only then initialize the additional (recursive) state of that object using the object itself.
 * For example, see how this is done for [ConeCapturedType] in the [ConeCapturedTypePointer.restoreConstructorSupertypes] function.
 *
 * Therefore, when we detect that we are trying to restore a recursive pointer ([RestorationState.InProgress]),
 * we throw an exception to break the cycle and signal that the restoration logic must be handled specially.
 */
internal class ConeTypeRecursionGuard {
    private val creationCache = mutableMapOf<ConeKotlinType, ConeTypePointerWrapper<*>>()
    private val restorationCache = mutableMapOf<ConeTypePointer<*>, RestorationState<*>>()

    private sealed class RestorationState<T : ConeKotlinType> {
        class InProgress<T : ConeKotlinType> : RestorationState<T>()
        class Completed<T : ConeKotlinType>(val type: T?) : RestorationState<T>()
    }

    fun <T : ConeKotlinType> createPointer(coneType: T, create: (T) -> ConeTypePointer<T>): ConeTypePointer<T> {
        val existingWrapper = creationCache[coneType]
        if (existingWrapper != null) {
            // Cycle detected - return a pointer that delegates to the wrapper
            @Suppress("UNCHECKED_CAST")
            return DelegatingConeTypePointer(existingWrapper as ConeTypePointerWrapper<T>)
        }

        // Create an uninitialized wrapper and add it to the cache
        val wrapper = ConeTypePointerWrapper<T>()
        creationCache[coneType] = wrapper

        val pointer = create(coneType)
        wrapper.initialize(pointer)

        return pointer
    }

    fun <T : ConeKotlinType> restorePointer(pointer: ConeTypePointer<T>, session: KaFirSession): T? {
        when (val state = restorationCache[pointer]) {
            is RestorationState.Completed<*> -> {
                // Already restored, return the cached result
                @Suppress("UNCHECKED_CAST")
                return state.type as T?
            }
            is RestorationState.InProgress<*> -> {
                errorWithAttachment("Unhandled recursive pointer restoration") {
                    withEntry("pointer type", pointer::class.simpleName)
                }
            }
            null -> {
                // Not yet started
            }
        }

        restorationCache[pointer] = RestorationState.InProgress<T>()
        val restoredType = pointer.restore(session, guard = this)
        restorationCache[pointer] = RestorationState.Completed(restoredType)

        return restoredType
    }
}

/**
 * A wrapper for [ConeTypePointer] that enables lazy initialization to break cycles during pointer creation.
 *
 * When creating pointers for recursive types (which can happen with [ConeCapturedType]), we need to handle cycles:
 * 1. When visiting a type that's already being processed, we return a [DelegatingConeTypePointer] that references this wrapper
 * 2. The wrapper is initially uninitialized (added to the cache immediately)
 * 3. After the actual pointer is created, the wrapper is initialized with it
 * 4. When the delegating pointer is later used for restoration, it delegates to this wrapper's actual pointer
 *
 * This two-level indirection allows us to create a reference to a pointer before the pointer itself is fully constructed,
 * thus breaking the cycle without causing infinite recursion.
 */
private class ConeTypePointerWrapper<T : ConeKotlinType> {
    private var pointer: ConeTypePointer<T>? = null
    private var initialized = false

    fun initialize(pointer: ConeTypePointer<T>) {
        require(!initialized) { "PointerWrapper already initialized" }
        this.pointer = pointer
        this.initialized = true
    }

    fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): T? {
        require(initialized) { "PointerWrapper not initialized" }
        val pointer = pointer ?: return null
        return guard.restorePointer(pointer, session)
    }
}

private class DelegatingConeTypePointer<T : ConeKotlinType>(private val wrapper: ConeTypePointerWrapper<T>) : ConeTypePointer<T> {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): T? {
        return wrapper.restore(session, guard)
    }
}

internal fun <T : ConeKotlinType> T.createPointer(
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard = ConeTypeRecursionGuard(),
): ConeTypePointer<T> {
    return guard.createPointer(coneType = this) { coneType ->
        @Suppress("UNCHECKED_CAST")
        when (coneType) {
            is ConeDynamicType -> ConeDynamicTypePointer
            is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullTypePointer(coneType, builder, guard)
            is ConeIntersectionType -> ConeIntersectionTypePointer(coneType, builder, guard)
            is ConeRawType -> ConeRawTypePointer(coneType, builder, guard)
            is ConeFlexibleType -> ConeFlexibleTypePointer(coneType, builder, guard)
            is ConeCapturedType -> ConeCapturedTypePointer(coneType, builder, guard)
            is ConeErrorType -> ConeErrorTypePointer(coneType, builder, guard)
            is ConeClassLikeType -> ConeClassLikeTypePointer(coneType, builder, guard)
            is ConeTypeParameterType -> ConeTypeParameterTypePointer(coneType, builder)
            is ConeTypeVariableType -> ConeTypeVariableTypePointer(coneType, builder)
            is ConeIntegerLiteralConstantType -> ConeIntegerLiteralConstantTypePointer(coneType, builder, guard)
            is ConeIntegerConstantOperatorType -> ConeIntegerConstantOperatorTypePointer(coneType)
            else -> ConeNeverRestoringTypePointer
        } as ConeTypePointer<T>
    }
}

/**
 * A pointer for the compiler type representation.
 * Does not hold references to internal compiler abstractions, so it can be restored in a different [KaSession].
 *
 * Meant to be used as a lower level abstraction inside [KaTypePointer]s.
 */
internal interface ConeTypePointer<out T : ConeKotlinType> {
    /**
     * Restores the original type when possible, using a recursion guard to handle recursive types.
     * Returns `null` if restoration is impossible.
     */
    fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard = ConeTypeRecursionGuard()): T?
}

private class ConeClassLikeTypePointer(
    coneType: ConeClassLikeType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeClassLikeType> {
    private val lookupTag = coneType.lookupTag
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder, guard) }
    private val isNullable = coneType.isMarkedNullable
    private val abbreviatedTypePointer = coneType.abbreviatedType?.createPointer(builder, guard)
    private val isTypeAlias = lookupTag.toSymbol(builder.rootSession) is FirTypeAliasSymbol

    // function types-specific attributes
    private val hasReceiverType = coneType.receiverType(builder.rootSession) != null
    private val contextParameterNumber = coneType.contextParameterNumberForFunctionType

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeClassLikeTypeImpl? {
        val classLikeSymbol = lookupTag.toSymbol(session.firSession) ?: return null
        when (classLikeSymbol) {
            is FirTypeAliasSymbol -> if (!isTypeAlias) return null
            is FirClassSymbol -> if (isTypeAlias) return null
        }

        val typeArguments = typeArgumentPointers.map { it.restore(session, guard) ?: return null }
        val abbreviatedType = abbreviatedTypePointer?.let { guard.restorePointer(it, session) ?: return null }

        val attributes = buildList {
            if (abbreviatedType != null) {
                add(AbbreviatedTypeAttribute(abbreviatedType))
            }
            if (hasReceiverType) {
                add(CompilerConeAttributes.ExtensionFunctionType)
            }
            if (contextParameterNumber != 0) {
                add(CompilerConeAttributes.ContextFunctionTypeParams(contextParameterNumber))
            }
        }

        return ConeClassLikeTypeImpl(
            lookupTag = lookupTag,
            typeArguments = typeArguments.toTypedArray(),
            isMarkedNullable = isNullable,
            attributes = ConeAttributes.create(attributes)
        )
    }
}

private class ConeTypeParameterTypePointer(
    coneType: ConeTypeParameterType,
    builder: KaSymbolByFirBuilder,
) : ConeTypePointer<ConeTypeParameterType> {
    private val typeParameterPointer = builder.classifierBuilder.buildTypeParameterSymbol(coneType.lookupTag.symbol).createPointer()
    private val isNullable = coneType.isMarkedNullable

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeTypeParameterType? {
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
    private val isMarkedNullable = coneType.isMarkedNullable

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = coneType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeTypeVariableType? {
        val typeParameterSymbol = typeParameterSymbolPointer?.let { it.restoreSymbol(session) ?: return null }

        val typeConstructor = ConeTypeVariableTypeConstructor(debugName, typeParameterSymbol?.firSymbol?.toLookupTag())
        return ConeTypeVariableType(isMarkedNullable, typeConstructor)
    }
}

private class ConeCapturedTypePointer(
    coneType: ConeCapturedType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeCapturedType> {
    private val captureStatus = coneType.constructor.captureStatus
    private val lowerTypePointer = coneType.constructor.lowerType?.createPointer(builder, guard)
    private val isMarkedNullable = coneType.isMarkedNullable
    private val coneProjectionPointer = ConeTypeProjectionPointer(coneType.constructor.projection, builder, guard)
    private val constructorSupertypePointers = coneType.constructor.supertypes?.map { it.createPointer(builder, guard) }

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = coneType.constructor.typeParameterMarker as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeCapturedType? {
        val lowerType = lowerTypePointer?.let { guard.restorePointer(it, session) ?: return null }
        val constructorProjection = coneProjectionPointer.restore(session, guard) ?: return null

        val constructorTypeParameterMarker = if (typeParameterSymbolPointer != null) {
            val typeParameterSymbol = with(session) { typeParameterSymbolPointer.restoreSymbol() } ?: return null
            typeParameterSymbol.firSymbol.toLookupTag()
        } else {
            null
        }

        val typeConstructor = ConeCapturedTypeConstructor(
            constructorProjection,
            lowerType,
            captureStatus,
            supertypes = null,
            constructorTypeParameterMarker
        )

        return ConeCapturedType(
            isMarkedNullable,
            typeConstructor,
        )
    }

    /**
     * A captured type may be recursive, so we must break its construction into two stages:
     * 1. Restore the [ConeCapturedType] object and remember it in the restoration cache
     * 2. Restore its supertypes, which may be the same as the type itself
     * 3. Make sure to *not* call this function for restored supertypes, as it would lead to an infinite loop
     */
    fun restoreConstructorSupertypes(coneType: ConeCapturedType, session: KaFirSession, guard: ConeTypeRecursionGuard) {
        val constructorSupertypes = constructorSupertypePointers?.restore(session, guard)
        coneType.constructor.supertypes = constructorSupertypes
    }
}

private class ConeIntersectionTypePointer(
    coneType: ConeIntersectionType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeIntersectionType> {
    private val intersectedTypePointers = coneType.intersectedTypes.map { it.createPointer(builder, guard) }
    private val upperBoundForApproximationPointer = coneType.upperBoundForApproximation?.createPointer(builder, guard)

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeIntersectionType? {
        val intersectedTypes = intersectedTypePointers.restore(session, guard) ?: return null
        val upperBoundForApproximation = upperBoundForApproximationPointer?.let { guard.restorePointer(it, session) ?: return null }

        return ConeIntersectionType(intersectedTypes, upperBoundForApproximation)
    }
}

private abstract class AbstractConeFlexibleTypePointer<T : ConeFlexibleType>(
    coneType: ConeFlexibleType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<T> {
    private val lowerBoundPointer = coneType.lowerBound.createPointer(builder, guard)
    private val upperBoundPointer = coneType.upperBound.createPointer(builder, guard)

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): T? {
        val lowerBound = guard.restorePointer(lowerBoundPointer, session) ?: return null
        val upperBound = guard.restorePointer(upperBoundPointer, session) ?: return null

        return restore(lowerBound, upperBound)
    }

    abstract fun restore(lowerBound: ConeRigidType, upperBound: ConeRigidType): T
}

private class ConeFlexibleTypePointer(
    coneType: ConeFlexibleType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : AbstractConeFlexibleTypePointer<ConeFlexibleType>(coneType, builder, guard) {
    override fun restore(lowerBound: ConeRigidType, upperBound: ConeRigidType): ConeFlexibleType {
        return ConeFlexibleType(lowerBound, upperBound, isTrivial = false)
    }
}

private class ConeRawTypePointer(
    coneType: ConeFlexibleType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : AbstractConeFlexibleTypePointer<ConeFlexibleType>(coneType, builder, guard) {
    override fun restore(lowerBound: ConeRigidType, upperBound: ConeRigidType): ConeFlexibleType {
        return ConeRawType.create(lowerBound, upperBound)
    }
}

private class ConeDefinitelyNotNullTypePointer(
    coneType: ConeDefinitelyNotNullType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeDefinitelyNotNullType> {
    private val originalTypePointer = coneType.original.createPointer(builder, guard)

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeDefinitelyNotNullType? {
        val originalType = guard.restorePointer(originalTypePointer, session) ?: return null
        return ConeDefinitelyNotNullType(originalType)
    }
}

private class ConeIntegerLiteralConstantTypePointer(
    coneType: ConeIntegerLiteralConstantType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeIntegerLiteralConstantType> {
    private val value = coneType.value
    private val possibleTypePointers = coneType.possibleTypes.map { it.createPointer(builder, guard) }
    private val isUnsigned = coneType.isUnsigned
    private val isMarkedNullable = coneType.isMarkedNullable

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeIntegerLiteralConstantType? {
        val possibleTypes = possibleTypePointers.restore(session, guard) ?: return null
        return ConeIntegerLiteralConstantTypeImpl(value, possibleTypes, isUnsigned, isMarkedNullable)
    }
}

private class ConeIntegerConstantOperatorTypePointer(
    coneType: ConeIntegerConstantOperatorType,
) : ConeTypePointer<ConeIntegerConstantOperatorType> {
    private val isUnsigned = coneType.isUnsigned
    private val isMarkedNullable = coneType.isMarkedNullable

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeIntegerConstantOperatorType {
        return ConeIntegerConstantOperatorTypeImpl(isUnsigned, isMarkedNullable)
    }
}

private class ConeErrorTypePointer(
    coneType: ConeErrorType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeErrorType> {
    private val isUninferredParameter = coneType.isUninferredParameter
    private val coneDiagnosticPointer = ConeDiagnosticPointer.create(coneType.diagnostic, builder)
    private val delegatedTypePointer = coneType.delegatedType?.createPointer(builder, guard)
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder, guard) }
    private val nullable = coneType.nullable

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeErrorType? {
        val coneDiagnostic = coneDiagnosticPointer.restore(session) ?: return null
        val delegatedConeType = delegatedTypePointer?.let { guard.restorePointer(it, session) ?: return null }
        val typeArguments = typeArgumentPointers.restore(session, guard) ?: return null

        return ConeErrorType(
            diagnostic = coneDiagnostic,
            isUninferredParameter = isUninferredParameter,
            delegatedType = delegatedConeType,
            typeArguments = typeArguments.toTypedArray(),
            nullable = nullable,
        )
    }
}

private object ConeDynamicTypePointer : ConeTypePointer<ConeDynamicType> {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeDynamicType {
        return ConeDynamicType.create(session.firSession)
    }
}

private object ConeNeverRestoringTypePointer : ConeTypePointer<ConeKotlinType> {
    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeKotlinType? {
        return null
    }
}

internal class ConeTypeProjectionPointer(
    projection: ConeTypeProjection,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard = ConeTypeRecursionGuard(),
) {
    private val kind = projection.kind
    private val typePointer: ConeTypePointer<*>? = projection.type?.createPointer(builder, guard)

    fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard = ConeTypeRecursionGuard()): ConeTypeProjection? {
        if (kind == STAR) {
            return ConeStarProjection
        }

        requireNotNull(typePointer)
        val type = guard.restorePointer(typePointer, session) ?: return null

        if (type is ConeCapturedType && typePointer !is DelegatingConeTypePointer<*>) {
            (typePointer as ConeCapturedTypePointer).restoreConstructorSupertypes(type, session, guard)
        }

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
private fun <T : ConeKotlinType> List<ConeTypePointer<T>>.restore(session: KaFirSession, guard: ConeTypeRecursionGuard): List<T>? {
    return restoreAll { guard.restorePointer(it, session) }
}

/**
 * Restores all type projection pointers in a list.
 * Returns `null` if at least one pointer cannot be restored.
 */
@JvmName("restoreTypeProjections")
private fun List<ConeTypeProjectionPointer>.restore(session: KaFirSession, guard: ConeTypeRecursionGuard): List<ConeTypeProjection>? {
    return restoreAll { it.restore(session, guard) }
}

private inline fun <T : Any, R : Any> List<T>.restoreAll(factory: (T) -> R?): List<R>? {
    return buildList(size) {
        for (pointer in this@restoreAll) {
            val item = factory(pointer) ?: return null
            add(item)
        }
    }
}
