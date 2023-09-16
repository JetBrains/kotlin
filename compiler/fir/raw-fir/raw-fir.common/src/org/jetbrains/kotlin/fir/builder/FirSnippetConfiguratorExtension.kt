/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.FirSnippetBuilder
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import kotlin.reflect.KClass

abstract class FirSnippetConfiguratorExtension(
    session: FirSession,
) : FirExtension(session), FirScriptCodeFragmentExtension<FirSnippetBuilder> {
    companion object {
        val NAME = FirExtensionPointName("SnippetConfigurator")
    }

    final override val name: FirExtensionPointName get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirSnippetConfiguratorExtension::class

    fun interface Factory : FirExtension.Factory<FirSnippetConfiguratorExtension>

}

val FirExtensionService.snippetConfigurators: List<FirSnippetConfiguratorExtension> by FirExtensionService.registeredExtensions()
