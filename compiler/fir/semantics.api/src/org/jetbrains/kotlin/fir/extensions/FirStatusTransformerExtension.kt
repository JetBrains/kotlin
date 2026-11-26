/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirCallableDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirClassLikeDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import kotlin.reflect.KClass

/**
 * This extension point allows to update declaration status during status resolve phase.
 * It contains two generic functions, which are invoked on all kinds of declarations:
 * - [transformCallableStatus]
 * - [transformClassLikeStatus]
 * And a bunch of specific [transformXXXStatus] for specific declarations, which could be overridden for more
 * granular execution.
 *
 * The compiler itself invokes these specific functions, and by default they delegate to corresponding generic functions.
 *
 * !!! Deprecation period !!!
 *
 * In scope of https://youtrack.jetbrains.com/issue/KT-91201 the single [FirDeclarationStatus] interface was split into
 * two more specific interfaces: [FirCallableDeclarationStatus] and [FirClassLikeDeclarationStatus]. So during this change
 * the old generic function [transformStatus(FirDeclarationStatus, FirDeclaration)] was split into two generic functions
 * mentioned above ([transformCallableStatus] and [transformClassLikeStatus]). To keep the compatibility with already existing
 * extensions, which were not yet migrated to the new API, old functions are preserved, so delegation of functions looks like
 * the following:
 *
 * (new) transformStatus(callable/classLikeStatus, declaration, containingClass, isLocal) ->
 * (old, deprecated) transformStatus(status, declaration, containingClass, isLocal) ->
 * (new) transformCallable/ClassLikeStatus(status, declaration) ->
 * (old, deprecated) transformStatus(status, declaration)
 *
 * So, if you're migrating your extension to the new API, make sure you don't leave any overrides of deprecated functions,
 * as they won't be invoked if new functions are overridden.
 *
 * !!! Deprecation period !!!
 */
@Suppress("DeprecatedCallableAddReplaceWith")
abstract class FirStatusTransformerExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("StatusTransformer")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirStatusTransformerExtension::class

    abstract fun needTransformStatus(declaration: FirDeclaration): Boolean

    protected open fun transformCallableStatus(
        status: FirCallableDeclarationStatus,
        declaration: FirCallableDeclaration
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status, declaration) as FirCallableDeclarationStatus
    }

    protected open fun transformClassLikeStatus(
        status: FirClassLikeDeclarationStatus,
        declaration: FirClassLikeDeclaration
    ): FirClassLikeDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status, declaration) as FirClassLikeDeclarationStatus
    }

    open fun transformPropertyStatus(
        status: FirCallableDeclarationStatus,
        property: FirProperty,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, property, containingClass, isLocal) as FirCallableDeclarationStatus
    }

    open fun transformFunctionStatus(
        status: FirCallableDeclarationStatus,
        function: FirNamedFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, function, containingClass, isLocal) as FirCallableDeclarationStatus
    }

    /**
     * This function may change the status (in general, visibility, modality, and modifiers) of a regular class.
     *
     * Limitation: it's forbidden for this extension to change the visibility of a regular class in any way,
     * as this may influence type resolve thus violating our phase contracts.
     */
    open fun transformClassStatus(
        status: FirClassLikeDeclarationStatus,
        regularClass: FirRegularClass,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirClassLikeDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, regularClass, containingClass, isLocal) as FirClassLikeDeclarationStatus
    }

    /**
     * This function may change the status (in general, visibility, modality, and modifiers) of a type alias.
     *
     * Limitation: it's forbidden for this extension to change the visibility of a type alias in any way,
     * as this may influence type resolve thus violating our phase contracts.
     */
    open fun transformTypeAliasStatus(
        status: FirClassLikeDeclarationStatus,
        typeAlias: FirTypeAlias,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirClassLikeDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, typeAlias, containingClass, isLocal) as FirClassLikeDeclarationStatus
    }

    open fun transformPropertyAccessorStatus(
        status: FirCallableDeclarationStatus,
        propertyAccessor: FirPropertyAccessor,
        containingClass: FirClassLikeSymbol<*>?,
        containingProperty: FirProperty?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, propertyAccessor, containingClass, containingProperty, isLocal) as FirCallableDeclarationStatus
    }

    open fun transformConstructorStatus(
        status: FirCallableDeclarationStatus,
        constructor: FirConstructor,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, constructor, containingClass, isLocal) as FirCallableDeclarationStatus
    }

    open fun transformFieldStatus(
        status: FirCallableDeclarationStatus,
        field: FirField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, field, containingClass, isLocal) as FirCallableDeclarationStatus
    }

    open fun transformBackingFieldStatus(
        status: FirCallableDeclarationStatus,
        backingField: FirBackingField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, backingField, containingClass, isLocal) as FirCallableDeclarationStatus
    }

    open fun transformEnumEntryStatus(
        status: FirCallableDeclarationStatus,
        enumEntry: FirEnumEntry,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirCallableDeclarationStatus {
        @Suppress("DEPRECATION")
        return transformStatus(status as FirDeclarationStatus, enumEntry, containingClass, isLocal) as FirCallableDeclarationStatus
    }

    // deprecated functions

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        declaration: FirDeclaration
    ): FirDeclarationStatus {
        return status
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        property: FirProperty,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, property)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        function: FirNamedFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, function)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        regularClass: FirRegularClass,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformClassLikeStatus(status as FirClassLikeDeclarationStatus, regularClass)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        typeAlias: FirTypeAlias,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformClassLikeStatus(status as FirClassLikeDeclarationStatus, typeAlias)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        propertyAccessor: FirPropertyAccessor,
        containingClass: FirClassLikeSymbol<*>?,
        containingProperty: FirProperty?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, propertyAccessor)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        constructor: FirConstructor,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, constructor)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        field: FirField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, field)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        backingField: FirBackingField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, backingField)
    }

    @Deprecated(DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    protected open fun transformStatus(
        status: FirDeclarationStatus,
        enumEntry: FirEnumEntry,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return transformCallableStatus(status as FirCallableDeclarationStatus, enumEntry)
    }

    fun interface Factory : FirExtension.Factory<FirStatusTransformerExtension>
}

val FirExtensionService.statusTransformerExtensions: List<FirStatusTransformerExtension> by FirExtensionService.registeredExtensions()

inline fun FirCallableDeclarationStatus.transform(
    visibility: Visibility = this.visibility,
    modality: Modality? = this.modality,
    init: FirCallableDeclarationStatusImpl.() -> Unit = {}
): FirDeclarationStatus {
    @Suppress("DuplicatedCode")
    return FirCallableDeclarationStatusImpl(visibility, modality).apply {
        isExpect = this@transform.isExpect
        isActual = this@transform.isActual
        isOverride = this@transform.isOverride
        isOperator = this@transform.isOperator
        isInfix = this@transform.isInfix
        isInline = this@transform.isInline
        isTailRec = this@transform.isTailRec
        isExternal = this@transform.isExternal
        isConst = this@transform.isConst
        isLateInit = this@transform.isLateInit
        isSuspend = this@transform.isSuspend
        isStatic = this@transform.isStatic
        isFromSealedClass = this@transform.isFromSealedClass
        isFromEnumClass = this@transform.isFromEnumClass
    }.apply(init)
}

inline fun FirClassLikeDeclarationStatus.transform(
    visibility: Visibility = this.visibility,
    modality: Modality? = this.modality,
    init: FirDeclarationStatusImpl.() -> Unit = {}
): FirClassLikeDeclarationStatus {
    @Suppress("DuplicatedCode")
    return FirClassLikeDeclarationStatusImpl(visibility, modality).apply {
        isExpect = this@transform.isExpect
        isActual = this@transform.isActual
        isInline = this@transform.isInline
        isValue = this@transform.isValue
        isExternal = this@transform.isExternal
        isInner = this@transform.isInner
        isCompanion = this@transform.isCompanion
        isData = this@transform.isData
        isFromSealedClass = this@transform.isFromSealedClass
        isFromEnumClass = this@transform.isFromEnumClass
    }.apply(init)
}

private const val DEPRECATION_MESSAGE = """
    |Methods working with `FirDeclarationStatus` are deprecated.
    |Use more specific methods with FirCallableDeclarationStatus or `FirClassLikeDeclarationStatus` instead.
    |Important: read the deprecation notes in the documentation to `FirStatusTransformerExtension`.
    """.trimMargin()
