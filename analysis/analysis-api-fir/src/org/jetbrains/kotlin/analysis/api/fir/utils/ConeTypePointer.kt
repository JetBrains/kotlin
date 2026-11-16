/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.fir.resolve.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ProjectionKind.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
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
 *
 * Since a type pointer is cached only once it is created, a cycle which breaks the invariant above would not terminate, so such a
 * cycle is detected with [inProgressMarker] instead. Restoration needs no such check: a type pointer is published only once it is
 * created, so the pointers form cycles only through registered captured type constructor pointers, which are restored in two steps.
 */
internal class ConeTypeRecursionGuard {
    /**
     * The pointers created for types and type constructors, by identity. A type whose pointer is being created maps to
     * [inProgressMarker] instead.
     */
    private val pointerCache = IdentityHashMap<Any, Any>()

    /**
     * The types and type constructors restored from pointers, by pointer identity. A value may be `null` if the restoration failed.
     */
    private val restorationCache = IdentityHashMap<Any, Any?>()

    /**
     * Marks the types in [pointerCache] whose pointers are being created in the current scope.
     *
     * The supertypes of each captured type constructor start a new scope with a fresh marker (see [createSupertypePointers]). A type
     * reached again from an inner scope is a cycle through [ConeCapturedTypeConstructor.supertypes], which ends at the registered
     * constructor pointer. A type reached again within a single scope is a cycle which breaks the invariant above.
     */
    private var inProgressMarker = Any()

    fun <T : ConeKotlinType> createPointer(coneType: T, create: (T) -> ConeTypePointer<T>): ConeTypePointer<T> {
        val cached = pointerCache[coneType]
        if (cached is ConeTypePointer<*>) {
            @Suppress("UNCHECKED_CAST")
            return cached as ConeTypePointer<T>
        }

        if (cached === inProgressMarker) {
            errorWithAttachment("Type pointer creation cycle which does not go through captured type constructor supertypes") {
                // The type itself is not rendered, as the renderer would follow the same cycle
                withEntry("typeClass", coneType::class.simpleName)
            }
        }

        pointerCache[coneType] = inProgressMarker
        return create(coneType).also { pointerCache[coneType] = it }
    }

    context(analysisSession: KaFirSession)
    fun <T : ConeKotlinType> restorePointer(pointer: ConeTypePointer<T>): T? {
        if (pointer in restorationCache) {
            @Suppress("UNCHECKED_CAST")
            return restorationCache[pointer] as T?
        }

        val restoredType = pointer.restore(guard = this)
        restorationCache[pointer] = restoredType
        return restoredType
    }

    fun createCapturedConstructorPointer(
        constructor: ConeCapturedTypeConstructor,
        builder: KaSymbolByFirBuilder,
    ): ConeCapturedTypeConstructorPointer {
        return pointerCache[constructor] as ConeCapturedTypeConstructorPointer?
            ?: ConeCapturedTypeConstructorPointer(constructor, builder, guard = this)
    }

    /**
     * Registers the [pointer] of a captured type [constructor] and creates the pointers for the constructor supertypes.
     *
     * The pointer is registered first, as the supertypes may refer back to the constructor, e.g., `Recursive<C>` for a captured type
     * `C`. Such a cycle ends at the registered pointer, so the supertypes start a new scope of [inProgressMarker].
     */
    fun createSupertypePointers(
        constructor: ConeCapturedTypeConstructor,
        pointer: ConeCapturedTypeConstructorPointer,
        builder: KaSymbolByFirBuilder,
    ): List<ConeTypePointer<*>>? {
        pointerCache[constructor] = pointer
        val supertypes = constructor.supertypes ?: return null

        val outerMarker = inProgressMarker
        inProgressMarker = Any()
        try {
            return supertypes.map { it.createPointer(builder, guard = this) }
        } finally {
            inProgressMarker = outerMarker
        }
    }

    context(analysisSession: KaFirSession)
    fun restoreCapturedConstructor(pointer: ConeCapturedTypeConstructorPointer): ConeCapturedTypeConstructor? {
        if (pointer in restorationCache) return restorationCache[pointer] as ConeCapturedTypeConstructor?

        val constructor = pointer.restoreWithoutSupertypes(guard = this)
        // Register the constructor before restoring the supertypes, as they may refer back to it
        restorationCache[pointer] = constructor
        constructor?.supertypes = pointer.supertypePointers?.restore(guard = this)
        return constructor
    }

    fun createTypeVariableConstructorPointer(
        constructor: ConeTypeVariableTypeConstructor,
        builder: KaSymbolByFirBuilder,
    ): ConeTypeVariableTypeConstructorPointer {
        return pointerCache.getOrPut(constructor) {
            ConeTypeVariableTypeConstructorPointer(constructor, builder)
        } as ConeTypeVariableTypeConstructorPointer
    }

    context(analysisSession: KaFirSession)
    fun restoreTypeVariableConstructor(
        pointer: ConeTypeVariableTypeConstructorPointer,
    ): ConeTypeVariableTypeConstructor? {
        if (pointer in restorationCache) return restorationCache[pointer] as ConeTypeVariableTypeConstructor?

        val constructor = pointer.restore()
        restorationCache[pointer] = constructor
        return constructor
    }
}

/**
 * Creates a pointer for this type. A nested pointer must be created with the [guard] of the enclosing pointer, as the guard shares
 * captured and type variable constructors within the whole type. A top-level pointer is created with a fresh guard.
 */
internal fun <T : ConeKotlinType> T.createPointer(
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
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
     * Restores the original type when possible, and returns `null` otherwise.
     *
     * A nested pointer must be restored with the [guard] of the enclosing pointer, as cycles through captured type constructor
     * supertypes end at the constructors registered in the guard. A top-level pointer is restored with a fresh guard.
     */
    context(analysisSession: KaFirSession)
    fun restore(guard: ConeTypeRecursionGuard): T?
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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeClassLikeTypeImpl? {
        val classLikeSymbol = lookupTag.toSymbol(analysisSession.firSession) ?: return null
        when (classLikeSymbol) {
            is FirTypeAliasSymbol -> if (!isTypeAlias) return null
            is FirClassSymbol -> if (isTypeAlias) return null
        }

        val typeArguments = typeArgumentPointers.map { it.restore(guard) ?: return null }
        val abbreviatedType = abbreviatedTypePointer?.let { guard.restorePointer(it) ?: return null }

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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeTypeParameterType? {
        val typeParameterSymbol = typeParameterPointer.restoreSymbol(analysisSession) ?: return null

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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeTypeVariableType? {
        val typeConstructor = guard.restoreTypeVariableConstructor(constructorPointer) ?: return null
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

    context(analysisSession: KaFirSession)
    fun restore(): ConeTypeVariableTypeConstructor? {
        val typeParameterSymbol = typeParameterSymbolPointer?.let { it.restoreSymbol() ?: return null }
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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeCapturedType? {
        val constructor = guard.restoreCapturedConstructor(constructorPointer) ?: return null
        return ConeCapturedType(isMarkedNullable, constructor)
    }
}

/**
 * A pointer to a [ConeCapturedTypeConstructor], shared by all captured types with this constructor.
 *
 * The pointer is registered with [ConeTypeRecursionGuard] before its [supertypePointers] are created, and the restored constructor's
 * supertypes are set after the constructor is created, since the supertypes may refer back to the constructor.
 */
internal class ConeCapturedTypeConstructorPointer(
    constructor: ConeCapturedTypeConstructor,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) {
    private val captureStatus = constructor.captureStatus
    private val lowerTypePointer = constructor.lowerType?.createPointer(builder, guard)
    private val projectionPointer = ConeTypeProjectionPointer(constructor.projection, builder, guard)

    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>? = run {
        val typeParameterLookupTag = constructor.typeParameterMarker as? ConeTypeParameterLookupTag
        if (typeParameterLookupTag != null) {
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameterLookupTag.symbol).createPointer()
        } else {
            null
        }
    }

    val supertypePointers: List<ConeTypePointer<*>>? = guard.createSupertypePointers(constructor, pointer = this, builder)

    context(analysisSession: KaFirSession)
    fun restoreWithoutSupertypes(guard: ConeTypeRecursionGuard): ConeCapturedTypeConstructor? {
        val lowerType = lowerTypePointer?.let { guard.restorePointer(it) ?: return null }
        val projection = projectionPointer.restore(guard) ?: return null

        val typeParameterMarker = if (typeParameterSymbolPointer != null) {
            val typeParameterSymbol = typeParameterSymbolPointer.restoreSymbol() ?: return null
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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeIntersectionType? {
        val intersectedTypes = intersectedTypePointers.restore(guard) ?: return null
        val upperBoundForApproximation = upperBoundForApproximationPointer?.let { guard.restorePointer(it) ?: return null }

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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): T? {
        val lowerBound = guard.restorePointer(lowerBoundPointer) ?: return null
        val upperBound = guard.restorePointer(upperBoundPointer) ?: return null

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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDefinitelyNotNullType? {
        val originalType = guard.restorePointer(originalTypePointer) ?: return null
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

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeIntegerLiteralConstantType? {
        val possibleTypes = possibleTypePointers.restore(guard) ?: return null
        return ConeIntegerLiteralConstantTypeImpl(value, possibleTypes, isUnsigned, isMarkedNullable)
    }
}

private class ConeIntegerConstantOperatorTypePointer(
    coneType: ConeIntegerConstantOperatorType,
) : ConeTypePointer<ConeIntegerConstantOperatorType> {
    private val isUnsigned = coneType.isUnsigned
    private val isMarkedNullable = coneType.isMarkedNullable

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeIntegerConstantOperatorType {
        return ConeIntegerConstantOperatorTypeImpl(isUnsigned, isMarkedNullable)
    }
}

private class ConeErrorTypePointer(
    coneType: ConeErrorType,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) : ConeTypePointer<ConeErrorType> {
    private val isUninferredParameter = coneType.isUninferredParameter
    private val coneDiagnosticPointer = ConeDiagnosticPointer.create(coneType.diagnostic, builder, guard)
    private val delegatedTypePointer = coneType.delegatedType?.createPointer(builder, guard)
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder, guard) }
    private val nullable = coneType.nullable

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeErrorType? {
        val coneDiagnostic = coneDiagnosticPointer.restore(guard) ?: return null
        val delegatedConeType = delegatedTypePointer?.let { guard.restorePointer(it) ?: return null }
        val typeArguments = typeArgumentPointers.restore(guard) ?: return null

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
    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDynamicType {
        return ConeDynamicType.create(analysisSession.firSession)
    }
}

private object ConeNeverRestoringTypePointer : ConeTypePointer<ConeKotlinType> {
    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeKotlinType? {
        return null
    }
}

internal class ConeTypeProjectionPointer(
    projection: ConeTypeProjection,
    builder: KaSymbolByFirBuilder,
    guard: ConeTypeRecursionGuard,
) {
    private val kind = projection.kind
    private val typePointer: ConeTypePointer<*>? = projection.type?.createPointer(builder, guard)

    context(analysisSession: KaFirSession)
    fun restore(guard: ConeTypeRecursionGuard): ConeTypeProjection? {
        if (kind == STAR) {
            return ConeStarProjection
        }

        requireNotNull(typePointer)
        val type = guard.restorePointer(typePointer) ?: return null

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
context(analysisSession: KaFirSession)
private fun <T : ConeKotlinType> List<ConeTypePointer<T>>.restore(guard: ConeTypeRecursionGuard): List<T>? {
    return restoreAll { guard.restorePointer(it) }
}

/**
 * Restores all type projection pointers in a list.
 * Returns `null` if at least one pointer cannot be restored.
 */
@JvmName("restoreTypeProjections")
context(analysisSession: KaFirSession)
private fun List<ConeTypeProjectionPointer>.restore(guard: ConeTypeRecursionGuard): List<ConeTypeProjection>? {
    return restoreAll { it.restore(guard) }
}

private inline fun <T : Any, R : Any> List<T>.restoreAll(factory: (T) -> R?): List<R>? {
    return buildList(size) {
        for (pointer in this@restoreAll) {
            val item = factory(pointer) ?: return null
            add(item)
        }
    }
}
