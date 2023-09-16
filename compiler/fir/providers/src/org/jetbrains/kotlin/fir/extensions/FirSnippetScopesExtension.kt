/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSnippet
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

abstract class FirSnippetScopesExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("SnippetScopesConfigurator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirSnippetScopesExtension::class

    fun interface Factory : FirExtension.Factory<FirSnippetScopesExtension>

    abstract fun contributeVariablesToReplScope(name: Name, processor: (FirVariableSymbol<*>) -> Unit)

    abstract fun contributeClassifiersToReplScope(name: Name, processor: (FirClassifierSymbol<*>) -> Unit)

    abstract fun contributeFunctionsToReplScope(name: Name, processor: (FirNamedFunctionSymbol) -> Unit)

    abstract fun registerVariables(firSnippet: FirSnippet, variables: List<FirVariableSymbol<*>>)
}

val FirExtensionService.snippetScopesConfigurators: List<FirSnippetScopesExtension> by FirExtensionService.registeredExtensions()
