/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.fir.FirSession

abstract class FirExtensionRegistrar {
    companion object : ProjectExtensionDescriptor<FirExtensionRegistrar>(
        "org.jetbrains.kotlin.fir.frontendIrExtension",
        FirExtensionRegistrar::class.java
    )

    protected inner class ExtensionRegistrarContext {
        operator fun ((FirSession) -> FirStatusTransformerExtension).unaryPlus() {
            statusTransformerExtensions += FirStatusTransformerExtension.Factory { this.invoke(it) }
        }
    }

    protected abstract fun ExtensionRegistrarContext.configurePlugin()

    fun configure(): RegisteredExtensions {
        ExtensionRegistrarContext().configurePlugin()
        return RegisteredExtensions(
            statusTransformerExtensions
        )
    }

    private val statusTransformerExtensions: MutableList<FirStatusTransformerExtension.Factory> = mutableListOf()

    class RegisteredExtensions(
        val statusTransformerExtensions: List<FirStatusTransformerExtension.Factory>
    ) {
        companion object {
            val EMPTY = RegisteredExtensions(emptyList())
        }
    }
}

fun FirExtensionPointService.registerExtensions(extensions: FirExtensionRegistrar.RegisteredExtensions) {
    registerExtensions(FirStatusTransformerExtension::class, extensions.statusTransformerExtensions)
}