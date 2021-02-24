/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import kotlin.reflect.KClass

abstract class FirSupertypeGenerationExtension(session: FirSession) : FirPredicateBasedExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("SupertypeGenerator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    override val extensionType: KClass<out FirExtension> = FirSupertypeGenerationExtension::class

    abstract fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        resolvedSupertypes: List<FirTypeRef>
    ): List<FirResolvedTypeRef>

    fun interface Factory : FirExtension.Factory<FirSupertypeGenerationExtension>
}

val FirExtensionService.supertypeGenerators: List<FirSupertypeGenerationExtension> by FirExtensionService.registeredExtensions()
