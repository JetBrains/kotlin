/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

abstract class FirExtensionRegistrar : FirExtensionRegistrarAdapter() {
    companion object {
        fun getInstances(project: Project): List<FirExtensionRegistrar> {
            @Suppress("UNCHECKED_CAST")
            return FirExtensionRegistrarAdapter.getInstances(project) as List<FirExtensionRegistrar>
        }

        fun registerExtension(project: Project, extension: FirExtensionRegistrar) {
            FirExtensionRegistrarAdapter.registerExtension(project, extension)
        }

        internal val AVAILABLE_EXTENSIONS = listOf(
            FirStatusTransformerExtension::class,
            FirDeclarationGenerationExtension::class,
            FirAdditionalCheckersExtension::class,
            FirSupertypeGenerationExtension::class,
            FirTypeAttributeExtension::class,
            FirExpressionResolutionExtension::class,
            FirExtensionSessionComponent::class,
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
    }

    @OptIn(PluginServicesInitialization::class)
    fun configure(): BunchOfRegisteredExtensions {
        if (isInitialized.compareAndSet(false, true)) {
            // Extension registrars can survive FirSession recreation in IDE mode, but we don't want to
            // call `configurePlugin` more than once, because it will lead to registering all plugins twice.
            // Please see KT-51444 for the details.

            ExtensionRegistrarContext().configurePlugin()
        }

        return BunchOfRegisteredExtensions(map.values)
    }

    class RegisteredExtensionsFactories(val kClass: KClass<out FirExtension>) {
        val extensionFactories: MutableList<FirExtension.Factory<FirExtension>> = mutableListOf()
    }

    private val map: Map<KClass<out FirExtension>, RegisteredExtensionsFactories> = AVAILABLE_EXTENSIONS.associateWith {
        RegisteredExtensionsFactories(it)
    }

    private var isInitialized: AtomicBoolean = AtomicBoolean(false)

    private fun <P : FirExtension> registerExtension(kClass: KClass<out P>, factory: FirExtension.Factory<P>) {
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
    extensionSessionComponents.forEach {
        session.register(it.componentClass, it)
    }
    session.registeredPluginAnnotations.initialize()
}
