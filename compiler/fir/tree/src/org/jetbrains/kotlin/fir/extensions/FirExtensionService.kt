/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.util.ConeTypeRegistry
import org.jetbrains.kotlin.util.ArrayMapAccessor
import org.jetbrains.kotlin.util.ComponentArrayOwner
import org.jetbrains.kotlin.util.TypeRegistry
import kotlin.reflect.KClass

@RequiresOptIn
annotation class PluginServicesInitialization

@NoMutableState
class FirExtensionService(val session: FirSession) : ComponentArrayOwner<FirExtension, List<FirExtension>>(), FirSessionComponent {
    companion object : ConeTypeRegistry<FirExtension, List<FirExtension>>() {
        inline fun <reified P : FirExtension, V : List<P>> registeredExtensions(): ArrayMapAccessor<FirExtension, List<FirExtension>, V> {
            @Suppress("UNCHECKED_CAST")
            return generateAccessor(P::class, default = emptyList<P>() as V)
        }

        fun <P : FirExtension, V : List<P>> registeredExtensions(kClass: KClass<P>): ArrayMapAccessor<FirExtension, List<FirExtension>, V> {
            @Suppress("UNCHECKED_CAST")
            return generateAccessor(kClass, default = emptyList<P>() as V)
        }
    }

    override val typeRegistry: TypeRegistry<FirExtension, List<FirExtension>>
        get() = Companion

    var registeredExtensionsSize: Int = 0
        private set

    @PluginServicesInitialization
    fun registerExtensions(extensionClass: KClass<out FirExtension>, extensionFactories: List<FirExtension.Factory<*>>) {
        registeredExtensionsSize += extensionFactories.size
        val extensions = extensionFactories.map { it.create(session) }
        registerComponent(
            extensionClass,
            extensions
        )
    }

    @PluginServicesInitialization
    fun getAllExtensions(): List<FirExtension> {
        return arrayMap.flatten()
    }
}

val FirSession.extensionService: FirExtensionService by FirSession.sessionComponentAccessor()
