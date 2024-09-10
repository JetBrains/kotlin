/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.scopes.FirScope
import kotlin.reflect.KClass

abstract class FirReplSnippetResolveExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("ReplSnippetConfigurator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirReplSnippetResolveExtension::class

    fun interface Factory : FirExtension.Factory<FirReplSnippetResolveExtension>

    abstract fun getSnippetScope(snippet: FirReplSnippet): FirScope?
}

val FirExtensionService.replSnippetResolveExtensions: List<FirReplSnippetResolveExtension> by FirExtensionService.registeredExtensions()
