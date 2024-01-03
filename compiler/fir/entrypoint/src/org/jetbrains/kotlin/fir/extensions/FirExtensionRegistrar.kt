/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.backend.Fir2IrScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.resolve.FirSamConversionTransformerExtension
import org.jetbrains.kotlin.fir.serialization.FirMetadataSerializerPlugin
import kotlin.reflect.KClass

abstract class FirExtensionRegistrar : FirExtensionRegistrarAdapter() {
    companion object {
        fun getInstances(project: Project): List<FirExtensionRegistrar> {
            @Suppress("UNCHECKED_CAST")
            return FirExtensionRegistrarAdapter.getInstances(project) as List<FirExtensionRegistrar>
        }

        internal val AVAILABLE_EXTENSIONS = listOf(
            FirStatusTransformerExtension::class,
            FirDeclarationGenerationExtension::class,
            FirAdditionalCheckersExtension::class,
            FirSupertypeGenerationExtension::class,
            FirTypeAttributeExtension::class,
            FirExpressionResolutionExtension::class,
            FirExtensionSessionComponent::class,
            FirSamConversionTransformerExtension::class,
            FirAssignExpressionAltererExtension::class,
            FirScriptConfiguratorExtension::class,
            Fir2IrScriptConfiguratorExtension::class,
            FirFunctionTypeKindExtension::class,
            @OptIn(FirExtensionApiInternals::class)
            FirMetadataSerializerPlugin::class,
        )

        internal val ALLOWED_EXTENSIONS_FOR_LIBRARY_SESSION = listOf(
            FirTypeAttributeExtension::class,
            FirFunctionTypeKindExtension::class,
        )
    }

    protected abstract fun ExtensionRegistrarContext.configurePlugin()

    protected inner class ExtensionRegistrarContext {
        // ------------------ factory methods ------------------

        @JvmName("plusStatusTransformerExtension")
        operator fun (FirStatusTransformerExtension.Factory).unaryPlus() {
            registerExtension(FirStatusTransformerExtension::class, this)
        }

        @JvmName("plusClassGenerationExtension")
        operator fun (FirDeclarationGenerationExtension.Factory).unaryPlus() {
            registerExtension(FirDeclarationGenerationExtension::class, this)
        }

        @JvmName("plusAdditionalCheckersExtension")
        operator fun (FirAdditionalCheckersExtension.Factory).unaryPlus() {
            registerExtension(FirAdditionalCheckersExtension::class, this)
        }

        @JvmName("plusSupertypeGenerationExtension")
        operator fun (FirSupertypeGenerationExtension.Factory).unaryPlus() {
            registerExtension(FirSupertypeGenerationExtension::class, this)
        }

        @JvmName("plusTypeAttributeExtension")
        operator fun (FirTypeAttributeExtension.Factory).unaryPlus() {
            registerExtension(FirTypeAttributeExtension::class, this)
        }

        @JvmName("plusExpressionResolutionExtension")
        operator fun (FirExpressionResolutionExtension.Factory).unaryPlus() {
            registerExtension(FirExpressionResolutionExtension::class, this)
        }

        @JvmName("plusExtensionSessionComponent")
        operator fun (FirExtensionSessionComponent.Factory).unaryPlus() {
            registerExtension(FirExtensionSessionComponent::class, this)
        }

        @JvmName("plusSamConversionTransformerExtension")
        operator fun (FirSamConversionTransformerExtension.Factory).unaryPlus() {
            registerExtension(FirSamConversionTransformerExtension::class, this)
        }

        @JvmName("plusAssignExpressionAltererExtension")
        operator fun (FirAssignExpressionAltererExtension.Factory).unaryPlus() {
            registerExtension(FirAssignExpressionAltererExtension::class, this)
        }

        @JvmName("plusScriptConfiguratorExtension")
        operator fun (FirScriptConfiguratorExtension.Factory).unaryPlus() {
            registerExtension(FirScriptConfiguratorExtension::class, this)
        }

        @JvmName("plusFir2IrScriptConfiguratorExtension")
        operator fun (Fir2IrScriptConfiguratorExtension.Factory).unaryPlus() {
            registerExtension(Fir2IrScriptConfiguratorExtension::class, this)
        }

        @JvmName("plusFunctionTypeKindExtension")
        operator fun (FirFunctionTypeKindExtension.Factory).unaryPlus() {
            registerExtension(FirFunctionTypeKindExtension::class, this)
        }

        @FirExtensionApiInternals
        @JvmName("plusMetadataSerializerPlugin")
        operator fun (FirMetadataSerializerPlugin.Factory).unaryPlus() {
            registerExtension(FirMetadataSerializerPlugin::class, this)
        }

        // ------------------ reference methods ------------------

        @JvmName("plusStatusTransformerExtension")
        operator fun ((FirSession) -> FirStatusTransformerExtension).unaryPlus() {
            FirStatusTransformerExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusClassGenerationExtension")
        operator fun ((FirSession) -> FirDeclarationGenerationExtension).unaryPlus() {
            FirDeclarationGenerationExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusAdditionalCheckersExtension")
        operator fun ((FirSession) -> FirAdditionalCheckersExtension).unaryPlus() {
            FirAdditionalCheckersExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusSupertypeGenerationExtension")
        operator fun ((FirSession) -> FirSupertypeGenerationExtension).unaryPlus() {
            FirSupertypeGenerationExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusTypeAttributeExtension")
        operator fun ((FirSession) -> FirTypeAttributeExtension).unaryPlus() {
            FirTypeAttributeExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusExpressionResolutionExtension")
        operator fun ((FirSession) -> FirExpressionResolutionExtension).unaryPlus() {
            FirExpressionResolutionExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusExtensionSessionComponent")
        operator fun ((FirSession) -> FirExtensionSessionComponent).unaryPlus() {
            FirExtensionSessionComponent.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusSamConversionTransformerExtension")
        operator fun ((FirSession) -> FirSamConversionTransformerExtension).unaryPlus() {
            FirSamConversionTransformerExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusAssignExpressionAltererExtension")
        operator fun ((FirSession) -> FirAssignExpressionAltererExtension).unaryPlus() {
            FirAssignExpressionAltererExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusScriptConfiguratorExtension")
        operator fun ((FirSession) -> FirScriptConfiguratorExtension).unaryPlus() {
            FirScriptConfiguratorExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusFir2IrScriptConfiguratorExtension")
        operator fun ((FirSession) -> Fir2IrScriptConfiguratorExtension).unaryPlus() {
            Fir2IrScriptConfiguratorExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusFunctionTypeKindExtension")
        operator fun ((FirSession) -> FirFunctionTypeKindExtension).unaryPlus() {
            FirFunctionTypeKindExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @FirExtensionApiInternals
        @JvmName("plusMetadataSerializerPlugin")
        operator fun ((FirSession) -> FirMetadataSerializerPlugin).unaryPlus() {
            FirMetadataSerializerPlugin.Factory { this.invoke(it) }.unaryPlus()
        }

        // ------------------ utilities ------------------

        @JvmName("bindLeft")
        fun <T, R> ((T, FirSession) -> R).bind(value: T): (FirSession) -> R {
            return { this.invoke(value, it) }
        }

        @JvmName("bindRight")
        fun <T, R> ((FirSession, T) -> R).bind(value: T): (FirSession) -> R {
            return { this.invoke(it, value) }
        }
    }

    @OptIn(PluginServicesInitialization::class)
    fun configure(): BunchOfRegisteredExtensions {
        return BunchOfRegisteredExtensions(configuredExtensionFactories)
    }

    private val extensionFactories: Map<KClass<out FirExtension>, MutableList<FirExtension.Factory<FirExtension>>> =
        AVAILABLE_EXTENSIONS.associateWith {
            mutableListOf()
        }

    /**
     * A lazy property which returns the [extensionFactories] map, but calls
     * [configurePlugin] to make sure that it's correctly configured.
     *
     * Extension registrars can survive FirSession recreation in IDE mode, but we don't want to
     * call [configurePlugin] more than once, because it will lead to registering all plugins twice.
     * That's why we don't want to call [configurePlugin] directly from the [configure].
     *
     * Instead, we use [lazy] to ensure that initialization happens only once, and that the
     * resulting [extensionFactories] map is visible to all possible callers, so no races occur.
     */
    private val configuredExtensionFactories: Map<KClass<out FirExtension>, List<FirExtension.Factory<FirExtension>>> by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        ExtensionRegistrarContext().configurePlugin()

        extensionFactories
    }

    private fun <P : FirExtension> registerExtension(kClass: KClass<out P>, factory: FirExtension.Factory<P>) {
        val registeredExtensions = extensionFactories.getValue(kClass)
        registeredExtensions += factory
    }
}

class BunchOfRegisteredExtensions @PluginServicesInitialization constructor(
    val extensions: Map<KClass<out FirExtension>, List<FirExtension.Factory<FirExtension>>>
) {
    companion object {
        @OptIn(PluginServicesInitialization::class)
        fun empty(): BunchOfRegisteredExtensions {
            return BunchOfRegisteredExtensions(FirExtensionRegistrar.AVAILABLE_EXTENSIONS.associateWith { listOf() })
        }
    }

    @OptIn(PluginServicesInitialization::class)
    operator fun plus(other: BunchOfRegisteredExtensions): BunchOfRegisteredExtensions {
        val combinedExtensions = buildMap {
            for (extensionClass in FirExtensionRegistrar.AVAILABLE_EXTENSIONS) {
                put(extensionClass, extensions.getValue(extensionClass) + other.extensions.getValue(extensionClass))
            }
        }
        return BunchOfRegisteredExtensions(combinedExtensions)
    }
}

@SessionConfiguration
@OptIn(PluginServicesInitialization::class)
fun FirExtensionService.registerExtensions(registeredExtensions: BunchOfRegisteredExtensions) {
    registeredExtensions.extensions.forEach { (extensionClass, extensionFactories) ->
        registerExtensions(extensionClass, extensionFactories)
    }
    extensionSessionComponents.forEach {
        session.register(it.componentClass, it)
    }
    session.registeredPluginAnnotations.initialize()
}
