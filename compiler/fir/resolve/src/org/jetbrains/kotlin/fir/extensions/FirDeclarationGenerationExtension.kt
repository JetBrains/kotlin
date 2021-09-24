/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
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

    // Can be called on SUPERTYPES stage
    open fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? = null

    // Can be called on STATUS stage
    open fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> = emptyList()
    open fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> = emptyList()
    open fun generateConstructors(callableId: CallableId): List<FirConstructorSymbol> = emptyList()

    // Can be called on IMPORTS stage
    open fun hasPackage(packageFqName: FqName): Boolean = false

    open fun getCallableNamesForGeneratedClass(classSymbol: FirClassSymbol<*>): Set<Name> = emptySet()
    open fun getNestedClassifiersNamesForGeneratedClass(classSymbol: FirClassSymbol<*>): Set<Name> = emptySet()

    fun interface Factory : FirExtension.Factory<FirDeclarationGenerationExtension>
}

val FirExtensionService.declarationGenerators: List<FirDeclarationGenerationExtension> by FirExtensionService.registeredExtensions()
