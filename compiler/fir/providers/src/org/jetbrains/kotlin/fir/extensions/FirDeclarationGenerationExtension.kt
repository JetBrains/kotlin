/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirLazyValue
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

/*
 * TODO:
 *  - check that annotations or meta-annotations is not empty
 */
abstract class FirDeclarationGenerationExtension(session: FirSession) : FirPredicateBasedExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("ExistingClassModification")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirDeclarationGenerationExtension::class

    /*
     * Can be called on SUPERTYPES stage
     *
     * If classId has `outerClassId.Companion` format then generated class should be a companion object
     */
    open fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? = null

    // Can be called on STATUS stage
    open fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> = emptyList()
    open fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> = emptyList()
    open fun generateConstructors(owner: FirClassSymbol<*>): List<FirConstructorSymbol> = emptyList()

    // Can be called on IMPORTS stage
    open fun hasPackage(packageFqName: FqName): Boolean = false

    /*
     * Can be called after SUPERTYPES stage
     *
     * `generate...` methods will be called only if `get...Names/ClassIds/CallableIds` returned corresponding
     *   declaration name
     *
     * If you want to generate constructor for some class, then you need to return `SpecialNames.INIT` in
     *   set of callable names for this class
     */
    open fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> = emptySet()
    open fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> = emptySet()
    open fun getTopLevelCallableIds(): Set<CallableId> = emptySet()
    open fun getTopLevelClassIds(): Set<ClassId> = emptySet()

    fun interface Factory : FirExtension.Factory<FirDeclarationGenerationExtension>

    // ----------------------------------- internal utils -----------------------------------

    @FirExtensionApiInternals
    val nestedClassifierNamesCache: FirCache<FirClassSymbol<*>, Set<Name>, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            getNestedClassifiersNames(symbol)
        }

    @FirExtensionApiInternals
    val topLevelClassIdsCache: FirLazyValue<Set<ClassId>, Nothing?> =
        session.firCachesFactory.createLazyValue { getTopLevelClassIds() }

    @FirExtensionApiInternals
    val topLevelCallableIdsCache: FirLazyValue<Set<CallableId>, Nothing?> =
        session.firCachesFactory.createLazyValue { getTopLevelCallableIds() }

}

val FirExtensionService.declarationGenerators: List<FirDeclarationGenerationExtension> by FirExtensionService.registeredExtensions()
