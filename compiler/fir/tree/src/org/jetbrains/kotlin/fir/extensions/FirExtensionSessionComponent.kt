/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import kotlin.reflect.KClass

abstract class FirExtensionSessionComponent(session: FirSession) : FirExtension(session), FirSessionComponent {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("ExtensionSessionComponent")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension>
        get() = FirExtensionSessionComponent::class

    open val componentClass: KClass<out FirExtensionSessionComponent>
        get() = this::class

    fun interface Factory : FirExtension.Factory<FirExtensionSessionComponent>
}

val FirExtensionService.extensionSessionComponents: List<FirExtensionSessionComponent> by FirExtensionService.registeredExtensions()
