/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClass
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

    abstract fun generateClasses(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<FirRegularClass>>

    abstract fun generateMembersForGeneratedClass(generatedClass: GeneratedClass): List<FirDeclaration>

    abstract fun generateMembers(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<*>>

    data class GeneratedDeclaration<out T : FirDeclaration>(val newDeclaration: T, val owner: FirAnnotatedDeclaration)

    fun interface Factory : FirExtension.Factory<FirDeclarationGenerationExtension>
}

val FirExtensionService.declarationGenerators: List<FirDeclarationGenerationExtension> by FirExtensionService.registeredExtensions()
