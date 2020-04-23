/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

abstract class FirExtensionRegistrarExtension {
    companion object : ProjectExtensionDescriptor<FirExtensionRegistrarExtension>(
        "org.jetbrains.kotlin.fir.frontendIrExtensionRegistrarExtension",
        FirExtensionRegistrarExtension::class.java
    )

    protected inner class ExtensionRegistrarContext

    protected abstract fun ExtensionRegistrarContext.configurePlugin()

    fun configure(): RegisteredExtensions {
        ExtensionRegistrarContext().configurePlugin()
        return RegisteredExtensions()
    }

    class RegisteredExtensions
}

fun FirExtensionPointService.registerExtensions(extensions: FirExtensionRegistrarExtension.RegisteredExtensions) {}