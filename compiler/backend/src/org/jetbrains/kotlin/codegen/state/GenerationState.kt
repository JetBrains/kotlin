/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.codegen.extensions.ClassGeneratorExtensionAdapter
import org.jetbrains.kotlin.codegen.inline.GlobalInlineContext
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilationComponents
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import java.lang.reflect.InvocationTargetException

class GenerationState(
    val project: Project,
    val module: ModuleDescriptor,
    val configuration: CompilerConfiguration,
    builderFactory: ClassBuilderFactory = ClassBuilderFactories.BINARIES,
    val generateDeclaredClassFilter: GenerateClassFilter? = null,
    val targetId: TargetId? = null,
    moduleName: String? = configuration.moduleName,
    val jvmBackendClassResolver: JvmBackendClassResolver,
    val ignoreErrors: Boolean = false,
    diagnosticReporter: DiagnosticReporter? = null,
    compiledCodeProvider: CompiledCodeProvider = CompiledCodeProvider.Empty
) {
    val diagnosticReporter: DiagnosticReporter =
        diagnosticReporter ?: DiagnosticsCollectorImpl()

    abstract class GenerateClassFilter {
        abstract fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGeneratePackagePart(ktFile: KtFile): Boolean
    }

    val config = JvmBackendConfig(configuration)

    val inlineCache: InlineCache = InlineCache(compiledCodeProvider)

    val incrementalCacheForThisTarget: IncrementalCache? = configuration.incrementalCompilationComponents?.let { components ->
        val targetId = targetId
            ?: moduleName?.let {
                // hack for Gradle IC, Gradle does not use build.xml file, so there is no way to pass target id
                TargetId(it, "java-production")
            } ?: error("Target ID should be specified for incremental compilation")
        components.getIncrementalCache(targetId)
    }

    val moduleName: String = moduleName ?: ModuleNameUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode

    val globalInlineContext: GlobalInlineContext = GlobalInlineContext()
    val factory: ClassFileFactory = ClassFileFactory(
        this,
        BuilderFactoryForDuplicateClassNameDiagnostics(
            if (classBuilderMode.generateBodies) OptimizationClassBuilderFactory(builderFactory, this) else builderFactory,
            this
        ).let {
            loadClassBuilderInterceptors().fold(it) { classBuilderFactory: ClassBuilderFactory, extension ->
                extension.interceptClassBuilderFactory(classBuilderFactory)
            }
        },
        configuration.getCompilerExtensions(ClassFileFactoryFinalizerExtension),
    )

    lateinit var mapInlineClass: (ClassDescriptor) -> Type

    lateinit var reportDuplicateClassNameError: (JvmDeclarationOrigin, String, JvmDeclarationOrigin) -> Unit

    lateinit var isDeclarationGeneratedForCompilerPlugin: (IrDeclaration) -> Boolean

    val newFragmentCaptureParameters: MutableList<Triple<String, KotlinType, DeclarationDescriptor>> = mutableListOf()

    @Suppress("UNCHECKED_CAST")
    private fun loadClassBuilderInterceptors(): List<ClassGeneratorExtensionAdapter> {
        val adapted = try {
            // Using Class.forName here because we're in the old JVM backend, and we need to load extensions declared in the JVM IR backend.
            Class.forName("org.jetbrains.kotlin.backend.jvm.extensions.ClassBuilderExtensionAdapter")
                .getDeclaredMethod("getExtensions", CompilerConfiguration::class.java)
                .invoke(null, configuration) as List<ClassGeneratorExtensionAdapter>
        } catch (e: InvocationTargetException) {
            // Unwrap and rethrow any exception that happens. It's important e.g. in case of ProcessCanceledException.
            throw e.targetException
        }

        return adapted
    }
}
