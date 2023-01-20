/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.builtins.functions.FunctionalTypeKind
import org.jetbrains.kotlin.fir.FirSession
import kotlin.reflect.KClass

abstract class FirFunctionalTypeKindExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("CustomFunctionalTypeKindExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension>
        get() = FirFunctionalTypeKindExtension::class

    interface FunctionalTypeKindRegistrar {
        fun registerKind(nonReflectKind: FunctionalTypeKind, reflectKind: FunctionalTypeKind)
    }

    abstract fun FunctionalTypeKindRegistrar.registerKinds()

    fun interface Factory : FirExtension.Factory<FirFunctionalTypeKindExtension>
}

val FirExtensionService.functionalTypeKindExtensions: List<FirFunctionalTypeKindExtension> by FirExtensionService.registeredExtensions()
