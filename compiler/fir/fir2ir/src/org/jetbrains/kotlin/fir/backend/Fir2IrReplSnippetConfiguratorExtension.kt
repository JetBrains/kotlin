/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import kotlin.reflect.KClass

abstract class Fir2IrReplSnippetConfiguratorExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("Fir2IrReplStateDeclarationsHandlerExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = Fir2IrReplSnippetConfiguratorExtension::class

    fun interface Factory : FirExtension.Factory<Fir2IrReplSnippetConfiguratorExtension>

    abstract fun Fir2IrComponents.prepareSnippet(fir2IrVisitor: Fir2IrVisitor, firReplSnippet: FirReplSnippet, irSnippet: IrReplSnippet)
}

val FirExtensionService.fir2IrReplSnippetConfigurators:
        List<Fir2IrReplSnippetConfiguratorExtension> by FirExtensionService.registeredExtensions()
