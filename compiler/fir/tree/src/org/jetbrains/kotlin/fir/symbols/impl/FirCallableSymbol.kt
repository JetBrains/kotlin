/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.mpp.CallableSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class FirCallableSymbol<out D : FirCallableDeclaration> : FirBasedSymbol<D>(), CallableSymbolMarker {
    abstract val callableId: CallableId

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
        get() = calculateReceiverTypeRef()

    private fun calculateReceiverTypeRef(): FirResolvedTypeRef? {
        val receiverParameter = fir.receiverParameter ?: return null
        ensureType(receiverParameter.typeRef)
        val receiverTypeRef = receiverParameter.typeRef
        if (receiverTypeRef !is FirResolvedTypeRef) {
            errorInLazyResolve("receiverTypeRef", receiverTypeRef::class, FirResolvedTypeRef::class)
        }

        return receiverTypeRef
    }

    val receiverParameter: FirReceiverParameter?
        get() {
            calculateReceiverTypeRef()
            return fir.receiverParameter
        }

    val resolvedContextParameters: List<FirValueParameter>
        get() {
            if (fir.contextParameters.isEmpty()) return emptyList()
            lazyResolveToPhase(FirResolvePhase.TYPES)
            return fir.contextParameters
        }

    val resolvedStatus: FirResolvedDeclarationStatus
        get() = fir.resolvedStatus()

    val rawStatus: FirDeclarationStatus
        get() = fir.status

    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.map { it.symbol }

    val dispatchReceiverType: ConeSimpleKotlinType?
        get() = fir.dispatchReceiverType

    val name: Name
        get() = callableId.callableName

    val containerSource: DeserializedContainerSource?
        // This is ok, because containerSource should be set during fir creation
        get() = fir.containerSource

    fun getDeprecation(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        if (deprecationsAreDefinitelyEmpty()) {
            return EmptyDeprecationsPerUseSite
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
        if (annotations.isEmpty() && fir.versionRequirements.isNullOrEmpty() && !rawStatus.isOverride) return true
        if (fir.deprecationsProvider == EmptyDeprecationsProvider) {
            return true
        }
        return false
    }

    private fun ensureType(typeRef: FirTypeRef?) {
        when (typeRef) {
            null, is FirResolvedTypeRef -> {}
            is FirImplicitTypeRef -> lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            else -> lazyResolveToPhase(FirResolvePhase.TYPES)
        }
    }

    override fun toString(): String = "${this::class.simpleName} $callableId"
}

val FirCallableSymbol<*>.isExtension: Boolean
    get() = when (fir) {
        is FirFunction -> fir.receiverParameter != null
        is FirProperty -> fir.receiverParameter != null
        is FirVariable -> false
    }
