/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

open class AbstractFirLoadCompiledJvmWithAnnotationsInMetadataKotlinTest : AbstractFirLoadK2CompiledJvmKotlinTest() {
    override val suppressDirective: SimpleDirective
        get() = Directives.IGNORE_FIR_METADATA_LOADING_K2_WITH_ANNOTATIONS_IN_METADATA

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+${LanguageFeature.AnnotationsInMetadata.name}"
            }
            useDirectives(Directives)
            useConfigurators(::ExtensionRegistrarConfigurator)
        }
    }

    internal object Directives : SimpleDirectivesContainer() {
        val IGNORE_FIR_METADATA_LOADING_K2_WITH_ANNOTATIONS_IN_METADATA by directive(
            description = "Ignore exceptions in AbstractFirLoadCompiledJvmWithAnnotationsInMetadataKotlinTest"
        )
    }
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    @ExperimentalCompilerApi
    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(RemoveAnnotationsExtension())
    }
}

// To check that annotations in metadata are loaded correctly, we erase annotations on declarations in the IR (and thus in the bytecode).
private class RemoveAnnotationsExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            file.acceptVoid(Visitor())
        }
    }

    private class Visitor : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            declaration.annotations = emptyList()
            super.visitClass(declaration)
        }

        override fun visitFunction(declaration: IrFunction) {
            declaration.annotations = emptyList()
            super.visitFunction(declaration)
        }

        override fun visitProperty(declaration: IrProperty) {
            declaration.annotations = emptyList()
            super.visitProperty(declaration)
        }

        override fun visitValueParameter(declaration: IrValueParameter) {
            declaration.annotations = emptyList()
            super.visitValueParameter(declaration)
        }
    }
}
