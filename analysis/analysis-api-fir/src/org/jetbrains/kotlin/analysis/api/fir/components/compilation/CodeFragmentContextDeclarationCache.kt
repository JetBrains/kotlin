/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmEvaluatorData
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * A cache for data to be passed between from the context file to the [KtCodeFragment].
 *
 * @param contextDeclaration A non-local declaration containing the context [PsiElement] of a code fragment.
 */
internal class CodeFragmentContextDeclarationCache(private val contextDeclaration: KtDeclaration) {
    /** A list of scope caches we accumulated when compiling the code fragment context. */
    private val collectedLocalScopes = mutableListOf<Fir2IrScopeCache>()

    /**
     * Registers declarations from the scope [cache] if the specified [symbol] is in the local scope of the [contextDeclaration].
     * It means this function only stores local declarations that are accessible from the code fragment.
     */
    fun registerLocalScope(symbol: IrSymbol, cache: Fir2IrScopeCache) {
        if (cache.isEmpty()) {
            return
        }

        val declaration = symbol.owner as? IrDeclaration ?: return

        /** Check if we are inside [contextDeclaration] */
        val isInsideContextDeclaration = generateSequence(declaration) { it.parent as? IrDeclaration }
            .filterIsInstance<IrMetadataSourceOwner>()
            .any { it.metadata?.source?.psi == contextDeclaration }

        if (isInsideContextDeclaration) {
            /** [Fir2IrScopeCache.clone] here is necessary as the scope cache is cleaned up before popping. */
            collectedLocalScopes.add(cache.clone())
        }
    }

    /**
     * Mapping between initial and desugared representations of local functions from the context module.
     * Currently, the map contains all local functions from the context module (including those outside the [contextDeclaration]).
     */
    var localDeclarationsData: JvmBackendContext.SharedLocalDeclarationsData? = null
        private set

    /**
     * A cache with declarations passed from the context module for the code fragment one.
     */
    var customCommonMemberStorage: Fir2IrCommonMemberStorage? = null
        private set

    fun initialize(fir2IrResult: Fir2IrActualizedResult, commonMemberStorage: Fir2IrCommonMemberStorage, evaluatorData: JvmEvaluatorData?) {
        require(localDeclarationsData == null && customCommonMemberStorage == null) { "Cache is already initialized" }

        localDeclarationsData = evaluatorData?.localDeclarationsData
        customCommonMemberStorage = computeStorage(fir2IrResult, commonMemberStorage)
    }

    /**
     * Collects local declarations from declaration storage of the context module.
     */
    private fun computeStorage(
        fir2IrResult: Fir2IrActualizedResult,
        commonMemberStorage: Fir2IrCommonMemberStorage
    ): Fir2IrCommonMemberStorage {
        val storage = Fir2IrCommonMemberStorage()

        /**
         * Register local classes.
         * Local classes have to be collected separately, as they are outside [Fir2IrComponents.declarationStorage].
         * */
        commonMemberStorage.localClassCache
            .forEach { (firClass, irClass) -> cacheSymbol(irClass, firClass, storage) }

        /**
         * Register other kinds of local declarations (type parameters, constructors, etc.)
         * Note that local functions and properties are inside [Fir2IrCommonMemberStorage.localCallableCache],
         * as the FIR2IR/backend doesn't even check the [Fir2IrComponents.declarationStorage] for local declarations.
         */
        @OptIn(DelicateDeclarationStorageApi::class)
        fir2IrResult.components.declarationStorage
            .forEachCachedDeclarationSymbol { irSymbol -> cacheSymbol(irSymbol, storage) }

        /**
         * Register local functions and properties.
         */
        storage.localCallableCache.addAll(collectedLocalScopes)

        return storage
    }

    private fun cacheSymbol(irSymbol: IrSymbol, storage: Fir2IrCommonMemberStorage) {
        val irDeclaration = (irSymbol.owner as? IrDeclaration)?.takeIf { it.isLocal } ?: return
        val metadata = (irDeclaration as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource ?: return
        cacheSymbol(irDeclaration, metadata.fir, storage)
    }

    private fun cacheSymbol(irDeclaration: IrDeclaration, firDeclaration: FirDeclaration, storage: Fir2IrCommonMemberStorage) {
        if (!shouldCacheSymbol(firDeclaration)) {
            return
        }

        /** Local functions and properties are cached in [Fir2IrCommonMemberStorage.localCallableCache]. */
        when (firDeclaration) {
            is FirClass -> storage.localClassCache[firDeclaration] = irDeclaration as IrClass
            is FirTypeParameter -> storage.typeParameterCache[firDeclaration] = irDeclaration as IrTypeParameter
            is FirEnumEntry -> storage.enumEntryCache[firDeclaration] = irDeclaration.symbol as IrEnumEntrySymbol
            is FirProperty -> {
                when (irDeclaration) {
                    is IrField -> firDeclaration.backingField?.let { backingField -> cacheSymbol(irDeclaration, backingField, storage) }
                    is IrProperty -> storage.propertyCache[firDeclaration] = irDeclaration.symbol
                }
            }
            is FirPropertyAccessor -> {
                val cache = if (firDeclaration.isGetter) storage.getterForPropertyCache else storage.setterForPropertyCache
                val irPropertySymbol = storage.propertyCache.getValue(firDeclaration.propertySymbol.fir)
                cache[irPropertySymbol] = irDeclaration.symbol as IrSimpleFunctionSymbol
            }
            is FirBackingField -> {
                val irPropertySymbol = storage.propertyCache.getValue(firDeclaration.propertySymbol.fir)
                storage.backingFieldForPropertyCache[irPropertySymbol] = irDeclaration.symbol as IrFieldSymbol
            }
            is FirConstructor -> storage.constructorCache[firDeclaration] = irDeclaration.symbol as IrConstructorSymbol
            is FirFunction -> storage.functionCache[firDeclaration] = irDeclaration.symbol as IrSimpleFunctionSymbol
            else -> {}
        }
    }

    private fun shouldCacheSymbol(firDeclaration: FirDeclaration): Boolean {
        /** Consistent with types of declarations cached by [Fir2IrScopeCache] */
        return when (firDeclaration) {
            is FirValueParameter -> shouldCacheSymbol(firDeclaration.containingDeclarationSymbol.fir)
            is FirPropertyAccessor -> shouldCacheSymbol(firDeclaration.propertySymbol.fir)
            is FirVariable -> !firDeclaration.isLocalMember
            is FirFunction -> !firDeclaration.isLocalMember
            else -> true
        }
    }
}