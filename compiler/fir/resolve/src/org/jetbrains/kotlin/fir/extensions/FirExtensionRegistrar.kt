/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.fir.FirSession
import kotlin.reflect.KClass

abstract class FirExtensionRegistrar {
    companion object : ProjectExtensionDescriptor<FirExtensionRegistrar>(
        "org.jetbrains.kotlin.fir.frontendIrExtension",
        FirExtensionRegistrar::class.java
    )

    protected abstract fun ExtensionRegistrarContext.configurePlugin()

    protected inner class ExtensionRegistrarContext {
        @JvmName("plusStatusTransformerExtension")
        operator fun ((FirSession) -> FirStatusTransformerExtension).unaryPlus() {
            registerExtension(FirStatusTransformerExtension::class, FirStatusTransformerExtension.Factory { this.invoke(it) })
        }

        @JvmName("plusClassGenerationExtension")
        operator fun ((FirSession) -> FirClassGenerationExtension).unaryPlus() {
            registerExtension(FirClassGenerationExtension::class, FirClassGenerationExtension.Factory { this.invoke(it) })
        }

        @JvmName("plusAdditionalCheckersExtension")
        operator fun ((FirSession) -> AbstractFirAdditionalCheckersExtension).unaryPlus() {
            registerExtension(
                AbstractFirAdditionalCheckersExtension::class,
                AbstractFirAdditionalCheckersExtension.Factory { this.invoke(it) }
            )
        }
    }

    fun configure(): Collection<RegisteredExtensionsFactories<*>> {
        ExtensionRegistrarContext().configurePlugin()
        return map.values
    }

    class RegisteredExtensionsFactories<P : FirExtension>(val kClass: KClass<P>) {
        val extensionFactories: MutableList<FirExtension.Factory<P>> = mutableListOf()
    }

    private val map: MutableMap<KClass<out FirExtension>, RegisteredExtensionsFactories<*>> = mutableMapOf()

    private fun <P : FirExtension> registerExtension(kClass: KClass<out P>, factory: FirExtension.Factory<P>) {
        @Suppress("UNCHECKED_CAST")
        val registeredExtensions = map.computeIfAbsent(kClass) { RegisteredExtensionsFactories(kClass) } as RegisteredExtensionsFactories<P>
        registeredExtensions.extensionFactories += factory
    }
}

@OptIn(PluginServicesInitialization::class)
fun FirExtensionService.registerExtensions(registeredExtensions: Collection<FirExtensionRegistrar.RegisteredExtensionsFactories<*>>) {
    registeredExtensions.forEach { registerExtensions(it.kClass, it.extensionFactories) }
    session.registeredPluginAnnotations.initialize()
}
