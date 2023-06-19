/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import kotlin.reflect.KClass

abstract class Fir2IrScriptConfiguratorExtension(
    session: FirSession,
) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("Fir2IrScriptConversion")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = Fir2IrScriptConfiguratorExtension::class

    fun interface Factory : FirExtension.Factory<Fir2IrScriptConfiguratorExtension>

    abstract fun IrScript.configure(script: FirScript, getIrScriptByFirSymbol: (FirScriptSymbol) -> IrScriptSymbol?)
}

val FirExtensionService.fir2IrScriptConfigurators: List<Fir2IrScriptConfiguratorExtension> by FirExtensionService.registeredExtensions()
