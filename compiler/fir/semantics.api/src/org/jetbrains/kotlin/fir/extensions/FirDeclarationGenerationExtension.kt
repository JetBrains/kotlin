/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirLazyValue
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirNestedClassifierScope
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

/**
 * All `generate*` members have the contract that the computation should be side-effect-free.
 * That means that all `generate*` function implementations should not modify any state or leak the generated `FirElement` or `FirBasedSymbol` (e.g., by putting it to some cache).
 * This restriction is imposed by the corresponding IDE cache implementation, which might retry the computation several times.
 */
abstract class FirDeclarationGenerationExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("ExistingClassModification")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirDeclarationGenerationExtension::class

    /*
     * Can be called on SUPERTYPES stage
     *
     * If classId has `outerClassId.Companion` format then generated class should be a companion object
     */
    @ExperimentalTopLevelDeclarationsGenerationApi
    open fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? = null

    /**
     * Can be called during [COMPANION_GENERATION][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.COMPANION_GENERATION] phase.
     *
     * If [name] is [DEFAULT_NAME_FOR_COMPANION_OBJECT][org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT],
     * the generated class must be a `companion object`.
     * For any other [Name], the generated class must not be a `companion object`.
     * Prefer generating `companion object` using [needsCompanionObject] instead.
     */
    open fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? = null

    // Can be called on STATUS stage
    open fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> = emptyList()

    /**
     * Special function designed for Java interop (to generate Java fields) and not useful for general plugins.
     */
    @UnsafePluginApi
    open fun generateFields(callableId: CallableId, context: MemberGenerationContext?): List<FirFieldSymbol> = emptyList()

    open fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> = emptyList()

    open fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> = emptyList()

    // Can be called on IMPORTS stage
    open fun hasPackage(packageFqName: FqName): Boolean = false

    /*
     * Can be called on STATUS stage
     *
     * `generate...` methods will be called only if `get...Names/ClassIds/CallableIds` returned corresponding
     *   declaration name
     *
     * If you want to generate constructor for some class, then you need to return `SpecialNames.INIT` in
     *   set of callable names for this class
     */
    open fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> = emptySet()

    /**
     * Called during [COMPANION_GENERATION][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.COMPANION_GENERATION] phase.
     *
     * The [generateNestedClassLikeDeclaration] function will be called for each corresponding [Name] returned by this function
     * when a [FirClassLikeSymbol] is needed for that name.
     *
     * To generate a `companion object`,
     * return [DEFAULT_NAME_FOR_COMPANION_OBJECT][org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT]
     * as part of the [Name] [Set].
     * However, prefer using [needsCompanionObject] whenever possible,
     * as it allows the `companion object` to be shared by multiple [FirDeclarationGenerationExtension]s.
     *
     * @see needsCompanionObject
     */
    open fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> = emptySet()

    /**
     * Called during [COMPANION_GENERATION][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.COMPANION_GENERATION] phase.
     *
     * If this function returns non-`null`, the compiler generates a `companion object` for [classSymbol].
     * All generators share that `companion object`, unlike a nested class from [getNestedClassifiersNames].
     */
    open fun needsCompanionObject(classSymbol: FirClassSymbol<*>, context: CompanionObjectGenerationContext): GeneratedDeclarationKey? = null

    @ExperimentalTopLevelDeclarationsGenerationApi
    open fun getTopLevelCallableIds(): Set<CallableId> = emptySet()

    @ExperimentalTopLevelDeclarationsGenerationApi
    open fun getTopLevelClassIds(): Set<ClassId> = emptySet()

    fun interface Factory : FirExtension.Factory<FirDeclarationGenerationExtension>

    // ----------------------------------- internal utils -----------------------------------

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    @FirExtensionApiInternals
    val topLevelClassIdsCache: FirLazyValue<Set<ClassId>> =
        session.firCachesFactory.createLazyValue { getTopLevelClassIds() }

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    @FirExtensionApiInternals
    val topLevelCallableIdsCache: FirLazyValue<Set<CallableId>> =
        session.firCachesFactory.createLazyValue { getTopLevelCallableIds() }

}

typealias MemberGenerationContext = DeclarationGenerationContext.Member
typealias NestedClassGenerationContext = DeclarationGenerationContext.Nested
typealias CompanionObjectGenerationContext = DeclarationGenerationContext.CompanionObject

sealed class DeclarationGenerationContext<T : FirContainingNamesAwareScope>(
    val owner: FirClassSymbol<*>,
    val declaredScope: T?,
    val typeResolver: PluginTypeResolver?,
) {
    // is needed for `hashCode` implementation
    protected abstract val kind: Int

    class Member(
        owner: FirClassSymbol<*>,
        declaredScope: FirClassDeclaredMemberScope?,
        typeResolver: PluginTypeResolver?,
    ) : DeclarationGenerationContext<FirClassDeclaredMemberScope>(owner, declaredScope, typeResolver) {
        override val kind: Int
            get() = 1
    }

    class Nested(
        owner: FirClassSymbol<*>,
        declaredScope: FirNestedClassifierScope?,
        typeResolver: PluginTypeResolver?,
    ) : DeclarationGenerationContext<FirNestedClassifierScope>(owner, declaredScope, typeResolver) {
        override val kind: Int
            get() = 2
    }

    class CompanionObject(
        owner: FirClassSymbol<*>,
        declaredScope: FirNestedClassifierScope?,
    ) : DeclarationGenerationContext<FirNestedClassifierScope>(owner, declaredScope, typeResolver = null) {
        override val kind: Int
            get() = 3
    }

    override fun equals(other: Any?): Boolean {
        if (this.javaClass !== other?.javaClass) {
            return false
        }
        require(other is DeclarationGenerationContext<*>)
        return owner == other.owner
    }

    override fun hashCode(): Int {
        return owner.hashCode() + kind
    }
}

val FirExtensionService.declarationGenerators: List<FirDeclarationGenerationExtension> by FirExtensionService.registeredExtensions()
