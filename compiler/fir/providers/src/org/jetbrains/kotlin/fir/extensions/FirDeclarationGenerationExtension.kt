/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
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
abstract class FirDeclarationGenerationExtension(session: FirSession) : FirExtension(session) {
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
    open fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? = null

    open fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? = null

    // Can be called on STATUS stage
    open fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> = emptyList()
    open fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> = emptyList()
    open fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> = emptyList()

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
    open fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> = emptySet()
    open fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> = emptySet()
    open fun getTopLevelCallableIds(): Set<CallableId> = emptySet()
    open fun getTopLevelClassIds(): Set<ClassId> = emptySet()

    fun interface Factory : FirExtension.Factory<FirDeclarationGenerationExtension>

    // ----------------------------------- internal utils -----------------------------------

    @FirExtensionApiInternals
    val nestedClassifierNamesCache: FirCache<FirClassSymbol<*>, Set<Name>, NestedClassGenerationContext> =
        session.firCachesFactory.createCache { symbol, context ->
            getNestedClassifiersNames(symbol, context)
        }

    @FirExtensionApiInternals
    val topLevelClassIdsCache: FirLazyValue<Set<ClassId>, Nothing?> =
        session.firCachesFactory.createLazyValue { getTopLevelClassIds() }

    @FirExtensionApiInternals
    val topLevelCallableIdsCache: FirLazyValue<Set<CallableId>, Nothing?> =
        session.firCachesFactory.createLazyValue { getTopLevelCallableIds() }

}

typealias MemberGenerationContext = DeclarationGenerationContext.Member
typealias NestedClassGenerationContext = DeclarationGenerationContext.Nested

sealed class DeclarationGenerationContext<T : FirContainingNamesAwareScope>(
    val owner: FirClassSymbol<*>,
    val declaredScope: T?,
) {
    // is needed for `hashCode` implementation
    protected abstract val kind: Int

    class Member(
        owner: FirClassSymbol<*>,
        declaredScope: FirClassDeclaredMemberScope?,
    ) : DeclarationGenerationContext<FirClassDeclaredMemberScope>(owner, declaredScope) {
        override val kind: Int
            get() = 1
    }

    class Nested(
        owner: FirClassSymbol<*>,
        declaredScope: FirNestedClassifierScope?,
    ) : DeclarationGenerationContext<FirNestedClassifierScope>(owner, declaredScope) {
        override val kind: Int
            get() = 2
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
