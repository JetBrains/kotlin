/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import kotlin.reflect.KClass

abstract class FirExtensionRegistrar {
    companion object {
        val AVAILABLE_EXTENSIONS = listOf(
            FirStatusTransformerExtension::class,
            FirDeclarationGenerationExtension::class,
            AbstractFirAdditionalCheckersExtension::class,
            FirSupertypeGenerationExtension::class
        )
    }

    protected abstract fun ExtensionRegistrarContext.configurePlugin()

    protected inner class ExtensionRegistrarContext {
        @JvmName("plusStatusTransformerExtension")
        operator fun ((FirSession) -> FirStatusTransformerExtension).unaryPlus() {
            registerExtension(FirStatusTransformerExtension::class, FirStatusTransformerExtension.Factory { this.invoke(it) })
        }

        @JvmName("plusClassGenerationExtension")
        operator fun ((FirSession) -> FirDeclarationGenerationExtension).unaryPlus() {
            registerExtension(FirDeclarationGenerationExtension::class, FirDeclarationGenerationExtension.Factory { this.invoke(it) })
        }

        @JvmName("plusAdditionalCheckersExtension")
        operator fun ((FirSession) -> AbstractFirAdditionalCheckersExtension).unaryPlus() {
            registerExtension(
                AbstractFirAdditionalCheckersExtension::class,
                AbstractFirAdditionalCheckersExtension.Factory { this.invoke(it) }
            )
        }

        @JvmName("plusSupertypeGenerationExtension")
        operator fun ((FirSession) -> FirSupertypeGenerationExtension).unaryPlus() {
            registerExtension(FirSupertypeGenerationExtension::class, FirSupertypeGenerationExtension.Factory { this.invoke(it) })
        }
    }

    @OptIn(PluginServicesInitialization::class)
    fun configure(): BunchOfRegisteredExtensions {
        ExtensionRegistrarContext().configurePlugin()
        return BunchOfRegisteredExtensions(map.values)
    }

    class RegisteredExtensionsFactories(val kClass: KClass<out FirExtension>) {
        val extensionFactories: MutableList<FirExtension.Factory<FirExtension>> = mutableListOf()
    }

    private val map: Map<KClass<out FirExtension>, RegisteredExtensionsFactories> = AVAILABLE_EXTENSIONS.map {
        it to RegisteredExtensionsFactories(it)
    }.toMap()

    private fun <P : FirExtension> registerExtension(kClass: KClass<out P>, factory: FirExtension.Factory<P>) {
        @Suppress("UNCHECKED_CAST")
        val registeredExtensions = map.getValue(kClass)
        registeredExtensions.extensionFactories += factory
    }
}

class BunchOfRegisteredExtensions @PluginServicesInitialization constructor(
    val extensions: Collection<FirExtensionRegistrar.RegisteredExtensionsFactories>
) {
    companion object {
        @OptIn(PluginServicesInitialization::class)
        fun empty(): BunchOfRegisteredExtensions {
            val extensions = FirExtensionRegistrar.AVAILABLE_EXTENSIONS.map { FirExtensionRegistrar.RegisteredExtensionsFactories(it) }
            return BunchOfRegisteredExtensions(extensions)
        }
    }

    @OptIn(PluginServicesInitialization::class)
    operator fun plus(other: BunchOfRegisteredExtensions): BunchOfRegisteredExtensions {
        return BunchOfRegisteredExtensions(extensions + other.extensions)
    }
}

@SessionConfiguration
@OptIn(PluginServicesInitialization::class)
fun FirExtensionService.registerExtensions(registeredExtensions: BunchOfRegisteredExtensions) {
    registeredExtensions.extensions.forEach { registerExtensions(it.kClass, it.extensionFactories) }
    session.registeredPluginAnnotations.initialize()
}
