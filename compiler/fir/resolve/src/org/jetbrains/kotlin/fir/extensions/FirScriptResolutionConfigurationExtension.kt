/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirScript
import kotlin.reflect.KClass

abstract class FirScriptResolutionConfigurationExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("FirScriptResolutionConfiguration")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirScriptResolutionConfigurationExtension::class

    fun interface Factory : FirExtension.Factory<FirScriptResolutionConfigurationExtension>

    abstract fun getScriptDefaultImports(script: FirScript): List<FirImport>
}

val FirExtensionService.firScriptResolutionConfigurators: List<FirScriptResolutionConfigurationExtension> by FirExtensionService.registeredExtensions()
