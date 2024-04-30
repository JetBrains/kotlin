/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import kotlin.reflect.KClass

abstract class FirSupertypeGenerationExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("SupertypeGenerator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirSupertypeGenerationExtension::class

    abstract fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean

    abstract fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<FirResolvedTypeRef>

    fun interface Factory : FirExtension.Factory<FirSupertypeGenerationExtension>

    abstract class TypeResolveService {
        abstract fun resolveUserType(type: FirUserTypeRef): FirResolvedTypeRef
    }
}

val FirExtensionService.supertypeGenerators: List<FirSupertypeGenerationExtension> by FirExtensionService.registeredExtensions()

