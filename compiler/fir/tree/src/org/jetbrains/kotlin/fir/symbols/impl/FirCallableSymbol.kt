/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.mpp.CallableSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.RequiresOptIn.Level.ERROR

abstract class FirCallableSymbol<out D : FirCallableDeclaration> : FirBasedSymbol<D>(), CallableSymbolMarker {
    /**
     * Combination of a package name, a container class name (if any) and a callable name.
     *
     * Under some circumstances can be used as an unique id (however, not recommended generally).
     * Equals null for local variables and parameters.
     */
    abstract val callableId: CallableId?

    /**
     * [callableId] having non-null value of [CallableId.PACKAGE_FQ_NAME_FOR_LOCAL].[name] for local variables/properties.
     *
     * Introduced specifically for rendering purposes. Please never use to identify something etc.
     */
    @RenderingInternals
    val callableIdForRendering: CallableId
        get() = callableId ?: CallableId(name)

    val resolvedReturnTypeRef: FirResolvedTypeRef
        get() {
            calculateReturnType()
            return fir.returnTypeRef as FirResolvedTypeRef
        }

    fun calculateReturnType() {
        ensureType(fir.returnTypeRef)
        val returnTypeRef = fir.returnTypeRef
        if (returnTypeRef !is FirResolvedTypeRef) {
            errorInLazyResolve("returnTypeRef", returnTypeRef::class, FirResolvedTypeRef::class)
        }
    }

    val resolvedReturnType: ConeKotlinType
        get() = resolvedReturnTypeRef.coneType

    val resolvedReceiverTypeRef: FirResolvedTypeRef?
        get() = receiverParameterSymbol?.calculateResolvedTypeRef()

    val resolvedReceiverType: ConeKotlinType?
        get() = resolvedReceiverTypeRef?.coneType

    val receiverParameterSymbol: FirReceiverParameterSymbol?
        get() = fir.receiverParameter?.symbol

    val contextParameterSymbols: List<FirValueParameterSymbol>
        get() = fir.contextParameters.map { it.symbol }

    val resolvedStatus: FirResolvedDeclarationStatus
        get() = fir.resolvedStatus()

    val rawStatus: FirDeclarationStatus
        get() = fir.status

    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.map { it.symbol }

    val ownTypeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.mapNotNull { (it as? FirTypeParameter)?.symbol }

    val dispatchReceiverType: ConeSimpleKotlinType?
        get() = fir.dispatchReceiverType

    abstract val name: Name

    val containerSource: DeserializedContainerSource?
        // This is ok, because containerSource should be set during fir creation
        get() = fir.containerSource

    fun getDeprecation(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        if (deprecationsAreDefinitelyEmpty()) {
            return null
        }

        lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        return fir.deprecationsProvider.getDeprecationsInfo(languageVersionSettings)
    }


    /**
     * Checks whether the deprecations of this declaration and of all other declarations that may affect this declaration are empty.
     *
     * This method may yield false negatives but guarantees no false positives.
     *
     * This method can traverse to parent or child declarations to perform the check.
     * In contrast, [currentDeclarationDeprecationsAreDefinitelyEmpty] only checks the current declaration
     * and does not traverse to other declarations.
     */
    protected open fun deprecationsAreDefinitelyEmpty(): Boolean {
        return currentDeclarationDeprecationsAreDefinitelyEmpty()
    }

    /**
     * Checks whether the deprecations of the current declaration are definitely empty.
     *
     * This method may yield false negatives but guarantees no false positives.
     *
     * Unlike [deprecationsAreDefinitelyEmpty], this method does not check other declarations
     * such as parent or child declarations.
     */
    internal fun currentDeclarationDeprecationsAreDefinitelyEmpty(): Boolean {
        moduleData.session.lazyDeclarationResolver.forbidLazyResolveInside {
            if (origin is FirDeclarationOrigin.Java) {
                // Java may perform lazy resolution when accessing FIR tree internals, see KT-55387
                return false
            }
            if (annotations.isEmpty() && fir.versionRequirements.isNullOrEmpty() && !rawStatus.isOverride) return true
            if (fir.deprecationsProvider == EmptyDeprecationsProvider) {
                return true
            }
            return false
        }
    }

    private fun ensureType(typeRef: FirTypeRef?) {
        when (typeRef) {
            null, is FirResolvedTypeRef -> {}
            is FirImplicitTypeRef -> lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            else -> lazyResolveToPhase(FirResolvePhase.TYPES)
        }
    }

    override fun toString(): String {
        val description = when (isBound) {
            true -> callableIdAsString()
            false -> "(unbound)"
        }
        return "${this::class.simpleName} $description"
    }

    fun callableIdAsString(): String = callableId?.toString() ?: "<local>/$name"
}

val FirCallableSymbol<*>.hasContextParameters: Boolean
    get() = fir.contextParameters.isNotEmpty()

@RequiresOptIn(
    level = ERROR,
    message = "This API is intended to be used specifically for diagnostics/dumps rendering. Please don't use in other places."
)
annotation class RenderingInternals