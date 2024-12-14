/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirScript
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
        val NAME: FirExtensionPointName = FirExtensionPointName("ScriptConfigurator")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirScriptConfiguratorExtension::class

    fun interface Factory : FirExtension.Factory<FirScriptConfiguratorExtension>

    /**
     * Should return true if the implementation recognizes the script
     */
    abstract fun accepts(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean

    /**
     * Called on building the FirFile containing the script. Could be used, e.g., to configure imports
     */
    abstract fun FirScriptBuilder.configureContainingFile(fileBuilder: FirFileBuilder)

    /**
     * Called on building the FirScript, after all contents already converted and added to the builder
     */
    abstract fun FirScriptBuilder.configure(sourceFile: KtSourceFile?, context: Context<PsiElement>)
}

val FirExtensionService.scriptConfigurators: List<FirScriptConfiguratorExtension> by FirExtensionService.registeredExtensions()
