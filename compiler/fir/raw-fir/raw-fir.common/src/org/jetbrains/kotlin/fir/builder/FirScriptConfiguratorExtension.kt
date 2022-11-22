/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptBuilder
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import kotlin.reflect.KClass

abstract class FirScriptConfiguratorExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("ScriptConfigurator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirScriptConfiguratorExtension::class

    fun interface Factory : FirExtension.Factory<FirScriptConfiguratorExtension>

    abstract fun FirScriptBuilder.configure(fileBuilder: FirFileBuilder)
}

val FirExtensionService.scriptConfigurators: List<FirScriptConfiguratorExtension> by FirExtensionService.registeredExtensions()
