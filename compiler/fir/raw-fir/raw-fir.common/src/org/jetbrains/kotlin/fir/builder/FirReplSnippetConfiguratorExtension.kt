/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import kotlin.reflect.KClass

abstract class FirReplSnippetConfiguratorExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("ReplSnippetConfigurator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirReplSnippetConfiguratorExtension::class

    fun interface Factory : FirExtension.Factory<FirReplSnippetConfiguratorExtension>

    abstract fun isReplSnippetsSource(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean
    abstract fun FirReplSnippetBuilder.configureContainingFile(fileBuilder: FirFileBuilder)
    abstract fun FirReplSnippetBuilder.configure(sourceFile: KtSourceFile?, context: Context<*>)

    /**
     * Allows mutating the statements of a `FirReplSnippet` `$$eval` function body as needed before
     * it is created. For example, this can be used to turn the last expression of the body into a
     * property to persist the result of the snippet.
     */
    abstract fun MutableList<FirElement>.configure(sourceFile: KtSourceFile?, scriptSource: KtSourceElement, context: Context<*>)
}

val FirExtensionService.replSnippetConfigurators: List<FirReplSnippetConfiguratorExtension> by FirExtensionService.registeredExtensions()
