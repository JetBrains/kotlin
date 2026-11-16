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
import org.jetbrains.kotlin.fir.resolve.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ProjectionKind.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import java.util.IdentityHashMap

/**
 * Shares state between the pointers of a single type, on creation and on restoration.
 *
 * Captured and type variable types are compared by constructor identity, so each of their constructors gets a single pointer,
 * which is restored to a single constructor. Thus, `C`, `C?`, and `@Anno C` keep sharing one constructor after restoration.
 *
 * The only cycles in cone types go through [ConeCapturedTypeConstructor.supertypes], which are set after the constructor is created,
 * e.g., `Recursive<C?>` for a captured type `C`. So a captured type constructor and its pointer are registered before their
 * supertypes are visited.
 *
 * Type pointers and restored types are cached by identity, which keeps the shape of the original type graph and never merges
 * types which are merely equal. A type is cached only once it is restored, so a type reached again via the supertypes while its
 * restoration is in progress is restored as an equal copy.
 */
internal class ConeTypeRecursionGuard {
    private val typePointers = IdentityHashMap<ConeKotlinType, ConeTypePointer<*>>()
    private val restoredTypes = IdentityHashMap<ConeTypePointer<*>, ConeKotlinType?>()

    private val capturedConstructorPointers = IdentityHashMap<ConeCapturedTypeConstructor, ConeCapturedTypeConstructorPointer>()
    private val restoredCapturedConstructors = IdentityHashMap<ConeCapturedTypeConstructorPointer, ConeCapturedTypeConstructor?>()

    private val typeVariableConstructorPointers =
        IdentityHashMap<ConeTypeVariableTypeConstructor, ConeTypeVariableTypeConstructorPointer>()
    private val restoredTypeVariableConstructors =
        IdentityHashMap<ConeTypeVariableTypeConstructorPointer, ConeTypeVariableTypeConstructor?>()

    fun <T : ConeKotlinType> createPointer(coneType: T, create: (T) -> ConeTypePointer<T>): ConeTypePointer<T> {
        @Suppress("UNCHECKED_CAST")
        return typePointers.getOrPut(coneType) { create(coneType) } as ConeTypePointer<T>
    }

    fun <T : ConeKotlinType> restorePointer(pointer: ConeTypePointer<T>, session: KaFirSession): T? {
        if (pointer in restoredTypes) {
            @Suppress("UNCHECKED_CAST")
            return restoredTypes[pointer] as T?
        }

        val restoredType = pointer.restore(session, guard = this)
        restoredTypes[pointer] = restoredType
        return restoredType
    }

    fun createCapturedConstructorPointer(
        constructor: ConeCapturedTypeConstructor,
        builder: KaSymbolByFirBuilder,
    ): ConeCapturedTypeConstructorPointer {
        capturedConstructorPointers[constructor]?.let { return it }

        val pointer = ConeCapturedTypeConstructorPointer(constructor, builder, guard = this)
        // Register the pointer before visiting the supertypes, as they may refer back to the constructor
        capturedConstructorPointers[constructor] = pointer
        pointer.supertypePointers = constructor.supertypes?.map { it.createPointer(builder, guard = this) }
        return pointer
    }

    fun restoreCapturedConstructor(pointer: ConeCapturedTypeConstructorPointer, session: KaFirSession): ConeCapturedTypeConstructor? {
        if (pointer in restoredCapturedConstructors) return restoredCapturedConstructors[pointer]

        val constructor = pointer.restoreWithoutSupertypes(session, guard = this)
        // Register the constructor before restoring the supertypes, as they may refer back to it
        restoredCapturedConstructors[pointer] = constructor
        constructor?.supertypes = pointer.supertypePointers?.restore(session, guard = this)
        return constructor
    }

    fun createTypeVariableConstructorPointer(
        constructor: ConeTypeVariableTypeConstructor,
        builder: KaSymbolByFirBuilder,
    ): ConeTypeVariableTypeConstructorPointer {
        return typeVariableConstructorPointers.getOrPut(constructor) { ConeTypeVariableTypeConstructorPointer(constructor, builder) }
    }

    fun restoreTypeVariableConstructor(
        pointer: ConeTypeVariableTypeConstructorPointer,
        session: KaFirSession,
    ): ConeTypeVariableTypeConstructor? {
        if (pointer in restoredTypeVariableConstructors) return restoredTypeVariableConstructors[pointer]

        val constructor = pointer.restore(session)
        restoredTypeVariableConstructors[pointer] = constructor
        return constructor
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
            is ConeTypeVariableType -> ConeTypeVariableTypePointer(coneType, builder, guard)
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
        return ConeTypeParameterType(lookupTag, isNullable)
    }
}

private class ConeTypeVariableTypePointer(
    coneType: ConeTypeVariableType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeTypeVariableType> {
    private val constructorPointer = guard.createTypeVariableConstructorPointer(coneType.typeConstructor, builder)
    private val isMarkedNullable = coneType.isMarkedNullable

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeTypeVariableType? {
        val typeConstructor = guard.restoreTypeVariableConstructor(constructorPointer, session) ?: return null
        return ConeTypeVariableType(isMarkedNullable, typeConstructor)
    }
}

internal class ConeTypeVariableTypeConstructorPointer(
    constructor: ConeTypeVariableTypeConstructor,
    builder: KaSymbolByFirBuilder,
) {
    private val debugName = constructor.debugName

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = constructor.originalTypeParameter as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    fun restore(session: KaFirSession): ConeTypeVariableTypeConstructor? {
        val typeParameterSymbol = typeParameterSymbolPointer?.let { it.restoreSymbol(session) ?: return null }
        return ConeTypeVariableTypeConstructor(debugName, typeParameterSymbol?.firSymbol?.toLookupTag())
    }
}

private class ConeCapturedTypePointer(
    coneType: ConeCapturedType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeCapturedType> {
    private val constructorPointer = guard.createCapturedConstructorPointer(coneType.constructor, builder)
    private val isMarkedNullable = coneType.isMarkedNullable

    override fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeCapturedType? {
        val constructor = guard.restoreCapturedConstructor(constructorPointer, session) ?: return null
        return ConeCapturedType(isMarkedNullable, constructor)
    }
}

/**
 * A pointer to a [ConeCapturedTypeConstructor], shared by all captured types with this constructor.
 *
 * [supertypePointers] are set by [ConeTypeRecursionGuard] after the pointer is created, and the restored constructor's supertypes
 * are set after the constructor is created, since the supertypes may refer back to the constructor.
 */
internal class ConeCapturedTypeConstructorPointer(
    constructor: ConeCapturedTypeConstructor,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) {
    private val captureStatus = constructor.captureStatus
    private val lowerTypePointer = constructor.lowerType?.createPointer(builder, guard)
    private val projectionPointer = ConeTypeProjectionPointer(constructor.projection, builder, guard)

    var supertypePointers: List<ConeTypePointer<*>>? = null

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = constructor.typeParameterMarker as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    fun restoreWithoutSupertypes(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeCapturedTypeConstructor? {
        val lowerType = lowerTypePointer?.let { guard.restorePointer(it, session) ?: return null }
        val projection = projectionPointer.restore(session, guard) ?: return null

        val typeParameterMarker = if (typeParameterSymbolPointer != null) {
            val typeParameterSymbol = with(session) { typeParameterSymbolPointer.restoreSymbol() } ?: return null
            typeParameterSymbol.firSymbol.toLookupTag()
        } else {
            null
        }

        return ConeCapturedTypeConstructor(projection, lowerType, captureStatus, supertypes = null, typeParameterMarker)
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

        @OptIn(DelicateIntersectionConstructor::class)
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
