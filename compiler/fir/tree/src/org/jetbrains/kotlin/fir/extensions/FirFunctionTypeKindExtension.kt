/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import kotlin.reflect.KClass

abstract class FirFunctionTypeKindExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("CustomFunctionTypeKindExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension>
        get() = FirFunctionTypeKindExtension::class

    interface FunctionTypeKindRegistrar {
        fun registerKind(nonReflectKind: FunctionTypeKind, reflectKind: FunctionTypeKind)
    }

    abstract fun FunctionTypeKindRegistrar.registerKinds()

    fun interface Factory : FirExtension.Factory<FirFunctionTypeKindExtension>
}

val FirExtensionService.functionTypeKindExtensions: List<FirFunctionTypeKindExtension> by FirExtensionService.registeredExtensions()
