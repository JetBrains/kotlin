/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.diagnostics.registeredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.backend.Fir2IrReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.backend.Fir2IrScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.resolve.FirSamConversionTransformerExtension
import org.jetbrains.kotlin.fir.serialization.FirMetadataSerializerPlugin
import kotlin.reflect.KClass

public abstract class FirExtensionRegistrar : FirExtensionRegistrarAdapter() {
    public companion object {
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
            FirScriptResolutionConfigurationExtension::class,
            Fir2IrScriptConfiguratorExtension::class,
            Fir2IrReplSnippetConfiguratorExtension::class,
            FirReplSnippetConfiguratorExtension::class,
            FirFunctionTypeKindExtension::class,
            @OptIn(FirExtensionApiInternals::class)
            FirMetadataSerializerPlugin::class,
            @OptIn(FirExtensionApiInternals::class)
            FirFunctionCallRefinementExtension::class,
        )

        internal val ALLOWED_EXTENSIONS_FOR_LIBRARY_SESSION = listOf(
            FirTypeAttributeExtension::class,
            FirFunctionTypeKindExtension::class,
        )

        public fun isLibrarySessionAllowedExtension(extension: KClass<out FirExtension>): Boolean {
            return extension in ALLOWED_EXTENSIONS_FOR_LIBRARY_SESSION
        }
    }

    protected abstract fun ExtensionRegistrarContext.configurePlugin()

    protected inner class ExtensionRegistrarContext {
        // ------------------ factory methods ------------------

        @JvmName("plusStatusTransformerExtension")
        public operator fun (FirStatusTransformerExtension.Factory).unaryPlus() {
            registerExtension(FirStatusTransformerExtension::class, this)
        }

        @JvmName("plusClassGenerationExtension")
        public operator fun (FirDeclarationGenerationExtension.Factory).unaryPlus() {
            registerExtension(FirDeclarationGenerationExtension::class, this)
        }

        @JvmName("plusAdditionalCheckersExtension")
        public operator fun (FirAdditionalCheckersExtension.Factory).unaryPlus() {
            registerExtension(FirAdditionalCheckersExtension::class, this)
        }

        @JvmName("plusSupertypeGenerationExtension")
        public operator fun (FirSupertypeGenerationExtension.Factory).unaryPlus() {
            registerExtension(FirSupertypeGenerationExtension::class, this)
        }

        @JvmName("plusTypeAttributeExtension")
        public operator fun (FirTypeAttributeExtension.Factory).unaryPlus() {
            registerExtension(FirTypeAttributeExtension::class, this)
        }

        @JvmName("plusExpressionResolutionExtension")
        public operator fun (FirExpressionResolutionExtension.Factory).unaryPlus() {
            registerExtension(FirExpressionResolutionExtension::class, this)
        }

        @JvmName("plusExtensionSessionComponent")
        public operator fun (FirExtensionSessionComponent.Factory).unaryPlus() {
            registerExtension(FirExtensionSessionComponent::class, this)
        }

        @JvmName("plusSamConversionTransformerExtension")
        public operator fun (FirSamConversionTransformerExtension.Factory).unaryPlus() {
            registerExtension(FirSamConversionTransformerExtension::class, this)
        }

        @JvmName("plusAssignExpressionAltererExtension")
        public operator fun (FirAssignExpressionAltererExtension.Factory).unaryPlus() {
            registerExtension(FirAssignExpressionAltererExtension::class, this)
        }

        @JvmName("plusScriptConfiguratorExtension")
        public operator fun (FirScriptConfiguratorExtension.Factory).unaryPlus() {
            registerExtension(FirScriptConfiguratorExtension::class, this)
        }

        @JvmName("plusFirScriptResolutionConfigurationExtension")
        public operator fun (FirScriptResolutionConfigurationExtension.Factory).unaryPlus() {
            registerExtension(FirScriptResolutionConfigurationExtension::class, this)
        }

        @JvmName("plusFir2IrScriptConfiguratorExtension")
        public operator fun (Fir2IrScriptConfiguratorExtension.Factory).unaryPlus() {
            registerExtension(Fir2IrScriptConfiguratorExtension::class, this)
        }

        @JvmName("plusFir2IrReplStateDeclarationsProviderExtension")
        public operator fun (Fir2IrReplSnippetConfiguratorExtension.Factory).unaryPlus() {
            registerExtension(Fir2IrReplSnippetConfiguratorExtension::class, this)
        }

        @JvmName("plusReplSnippetConfiguratorExtension")
        public operator fun (FirReplSnippetConfiguratorExtension.Factory).unaryPlus() {
            registerExtension(FirReplSnippetConfiguratorExtension::class, this)
        }

        @JvmName("plusFunctionTypeKindExtension")
        public operator fun (FirFunctionTypeKindExtension.Factory).unaryPlus() {
            registerExtension(FirFunctionTypeKindExtension::class, this)
        }

        @FirExtensionApiInternals
        @JvmName("plusMetadataSerializerPlugin")
        public operator fun (FirMetadataSerializerPlugin.Factory).unaryPlus() {
            registerExtension(FirMetadataSerializerPlugin::class, this)
        }

        @FirExtensionApiInternals
        @JvmName("plusFunctionCallRefinementExtension")
        public operator fun (FirFunctionCallRefinementExtension.Factory).unaryPlus() {
            registerExtension(FirFunctionCallRefinementExtension::class, this)
        }

        // ------------------ reference methods ------------------

        @JvmName("plusStatusTransformerExtension")
        public operator fun ((FirSession) -> FirStatusTransformerExtension).unaryPlus() {
            FirStatusTransformerExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusClassGenerationExtension")
        public operator fun ((FirSession) -> FirDeclarationGenerationExtension).unaryPlus() {
            FirDeclarationGenerationExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusAdditionalCheckersExtension")
        public operator fun ((FirSession) -> FirAdditionalCheckersExtension).unaryPlus() {
            FirAdditionalCheckersExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusSupertypeGenerationExtension")
        public operator fun ((FirSession) -> FirSupertypeGenerationExtension).unaryPlus() {
            FirSupertypeGenerationExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusTypeAttributeExtension")
        public operator fun ((FirSession) -> FirTypeAttributeExtension).unaryPlus() {
            FirTypeAttributeExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusExpressionResolutionExtension")
        public operator fun ((FirSession) -> FirExpressionResolutionExtension).unaryPlus() {
            FirExpressionResolutionExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusExtensionSessionComponent")
        public operator fun ((FirSession) -> FirExtensionSessionComponent).unaryPlus() {
            FirExtensionSessionComponent.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusSamConversionTransformerExtension")
        public operator fun ((FirSession) -> FirSamConversionTransformerExtension).unaryPlus() {
            FirSamConversionTransformerExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusAssignExpressionAltererExtension")
        public operator fun ((FirSession) -> FirAssignExpressionAltererExtension).unaryPlus() {
            FirAssignExpressionAltererExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusScriptConfiguratorExtension")
        public operator fun ((FirSession) -> FirScriptConfiguratorExtension).unaryPlus() {
            FirScriptConfiguratorExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusFirScriptResolutionConfigurationExtension")
        public operator fun ((FirSession) -> FirScriptResolutionConfigurationExtension).unaryPlus() {
            FirScriptResolutionConfigurationExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusFir2IrScriptConfiguratorExtension")
        public operator fun ((FirSession) -> Fir2IrScriptConfiguratorExtension).unaryPlus() {
            Fir2IrScriptConfiguratorExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusFir2IrReplStateDeclarationsProviderExtension")
        public operator fun ((FirSession) -> Fir2IrReplSnippetConfiguratorExtension).unaryPlus() {
            Fir2IrReplSnippetConfiguratorExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusReplSnippetConfiguratorExtension")
        public operator fun ((FirSession) -> FirReplSnippetConfiguratorExtension).unaryPlus() {
            FirReplSnippetConfiguratorExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @JvmName("plusFunctionTypeKindExtension")
        public operator fun ((FirSession) -> FirFunctionTypeKindExtension).unaryPlus() {
            FirFunctionTypeKindExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        @FirExtensionApiInternals
        @JvmName("plusMetadataSerializerPlugin")
        public operator fun ((FirSession) -> FirMetadataSerializerPlugin).unaryPlus() {
            FirMetadataSerializerPlugin.Factory { this.invoke(it) }.unaryPlus()
        }

        @FirExtensionApiInternals
        @JvmName("plusFunctionCallRefinementExtension")
        public operator fun ((FirSession) -> FirFunctionCallRefinementExtension).unaryPlus() {
            FirFunctionCallRefinementExtension.Factory { this.invoke(it) }.unaryPlus()
        }

        // ------------------ diagnostics ------------------

        public fun registerDiagnosticContainers(vararg diagnosticContainers: KtDiagnosticsContainer) {
            this@FirExtensionRegistrar.diagnosticsContainers += diagnosticContainers
        }

        // ------------------ utilities ------------------

        @JvmName("bindLeft")
        public fun <T, R> ((T, FirSession) -> R).bind(value: T): (FirSession) -> R {
            return { this.invoke(value, it) }
        }

        @JvmName("bindRight")
        public fun <T, R> ((FirSession, T) -> R).bind(value: T): (FirSession) -> R {
            return { this.invoke(it, value) }
        }
    }

    @OptIn(PluginServicesInitialization::class)
    public fun configure(): BunchOfRegisteredExtensions {
        return BunchOfRegisteredExtensions(configuredExtensionFactories, diagnosticsContainers)
    }

    private val extensionFactories: Map<KClass<out FirExtension>, MutableList<FirExtension.Factory<FirExtension>>> =
        AVAILABLE_EXTENSIONS.associateWith {
            mutableListOf()
        }

    private val diagnosticsContainers: MutableList<KtDiagnosticsContainer> = mutableListOf()

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

public class BunchOfRegisteredExtensions @PluginServicesInitialization constructor(
    public val extensions: Map<KClass<out FirExtension>, List<FirExtension.Factory<FirExtension>>>,
    public val diagnosticsContainers: List<KtDiagnosticsContainer>
) {
    public companion object {
        @OptIn(PluginServicesInitialization::class)
        public fun empty(): BunchOfRegisteredExtensions {
            return BunchOfRegisteredExtensions(
                extensions = FirExtensionRegistrar.AVAILABLE_EXTENSIONS.associateWith { listOf() },
                diagnosticsContainers = emptyList()
            )
        }
    }

    @OptIn(PluginServicesInitialization::class)
    public operator fun plus(other: BunchOfRegisteredExtensions): BunchOfRegisteredExtensions {
        val combinedExtensions = buildMap {
            for (extensionClass in FirExtensionRegistrar.AVAILABLE_EXTENSIONS) {
                put(extensionClass, extensions.getValue(extensionClass) + other.extensions.getValue(extensionClass))
            }
        }
        val diagnosticContainers = diagnosticsContainers + other.diagnosticsContainers
        return BunchOfRegisteredExtensions(combinedExtensions, diagnosticContainers)
    }
}

@SessionConfiguration
@OptIn(PluginServicesInitialization::class)
public fun FirExtensionService.registerExtensions(registeredExtensions: BunchOfRegisteredExtensions) {
    registeredExtensions.extensions.forEach { [extensionClass, extensionFactories] ->
        registerExtensions(extensionClass, extensionFactories)
    }
    extensionSessionComponents.forEach {
        session.register(it.componentClass, it)
    }
    session.registeredPluginAnnotations.initialize()
    if (session.kind == FirSession.Kind.Source) {
        session.registeredDiagnosticFactoriesStorage?.registerDiagnosticContainers(registeredExtensions.diagnosticsContainers)
    }
}

