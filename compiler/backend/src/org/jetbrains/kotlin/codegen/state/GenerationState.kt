/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.codegen.inline.GlobalInlineContext
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilationComponents
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.load.java.components.JavaDeprecationSettings
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
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
    private val onIndependentPartCompilationEnd: (GenerationState) -> Unit = {},
    val jvmBackendClassResolver: JvmBackendClassResolver = JvmBackendClassResolverForModuleWithDependencies(module),
    val ignoreErrors: Boolean = false,
    diagnosticReporter: BaseDiagnosticsCollector? = null,
    compiledCodeProvider: CompiledCodeProvider = CompiledCodeProvider.Empty
) {
    val diagnosticReporter: BaseDiagnosticsCollector =
        diagnosticReporter ?: DiagnosticReporterFactory.createReporter(configuration.messageCollector)

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

    val deprecationProvider = DeprecationResolver(
        LockBasedStorageManager.NO_LOCKS, config.languageVersionSettings, JavaDeprecationSettings
    )

    val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode
    val bindingTrace: BindingTrace = DelegatingBindingTrace(BindingContext.EMPTY, "trace in GenerationState")
    val localDelegatedProperties: MutableMap<Type, List<VariableDescriptorWithAccessors>> = mutableMapOf()

    val globalInlineContext: GlobalInlineContext = GlobalInlineContext()
    val factory: ClassFileFactory = ClassFileFactory(
        this,
        BuilderFactoryForDuplicateClassNameDiagnostics(
            if (classBuilderMode.generateBodies) OptimizationClassBuilderFactory(builderFactory, this) else builderFactory,
            this
        ).let {
            loadClassBuilderInterceptors().fold(it) { classBuilderFactory: ClassBuilderFactory, extension ->
                extension.interceptClassBuilderFactory(classBuilderFactory, BindingContext.EMPTY, DiagnosticSink.DO_NOTHING)
            }
        },
        ClassFileFactoryFinalizerExtension.getInstances(project),
    )

    val globalSerializationBindings = JvmSerializationBindings()
    lateinit var mapInlineClass: (ClassDescriptor) -> Type

    class MultiFieldValueClassUnboxInfo(val unboxedTypesAndMethodNamesAndFieldNames: List<Triple<Type, String, String>>) {
        val unboxedTypes = unboxedTypesAndMethodNamesAndFieldNames.map { (type, _, _) -> type }
        val unboxedMethodNames = unboxedTypesAndMethodNamesAndFieldNames.map { (_, methodName, _) -> methodName }
    }

    var multiFieldValueClassUnboxInfo: (ClassDescriptor) -> MultiFieldValueClassUnboxInfo? = { null }

    lateinit var reportDuplicateClassNameError: (JvmDeclarationOrigin, String, String) -> Unit

    val typeApproximator: TypeApproximator = TypeApproximator(module.builtIns, config.languageVersionSettings)

    val newFragmentCaptureParameters: MutableList<Triple<String, KotlinType, DeclarationDescriptor>> = mutableListOf()

    @Suppress("UNCHECKED_CAST", "DEPRECATION_ERROR")
    private fun loadClassBuilderInterceptors(): List<org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension> {
        val adapted = try {
            // Using Class.forName here because we're in the old JVM backend, and we need to load extensions declared in the JVM IR backend.
            Class.forName("org.jetbrains.kotlin.backend.jvm.extensions.ClassBuilderExtensionAdapter")
                .getDeclaredMethod("getExtensions", Project::class.java)
                .invoke(null, project) as List<org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension>
        } catch (e: InvocationTargetException) {
            // Unwrap and rethrow any exception that happens. It's important e.g. in case of ProcessCanceledException.
            throw e.targetException
        }

        return org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension.getInstances(project) + adapted
    }

    fun afterIndependentPart() {
        onIndependentPartCompilationEnd(this)
    }
}
