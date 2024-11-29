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
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.java.components.JavaDeprecationSettings
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.org.objectweb.asm.Type
import java.io.File
import java.lang.reflect.InvocationTargetException

class GenerationState private constructor(
    val project: Project,
    builderFactory: ClassBuilderFactory,
    val module: ModuleDescriptor,
    val configuration: CompilerConfiguration,
    val generateDeclaredClassFilter: GenerateClassFilter,
    val targetId: TargetId?,
    moduleName: String?,
    val outDirectory: File?,
    private val onIndependentPartCompilationEnd: GenerationStateEventCallback,
    val jvmBackendClassResolver: JvmBackendClassResolver,
    val ignoreErrors: Boolean,
    val diagnosticReporter: DiagnosticReporter,
) {
    class Builder(
        private val project: Project,
        private val builderFactory: ClassBuilderFactory,
        private val module: ModuleDescriptor,
        private val configuration: CompilerConfiguration
    ) {
        private var generateDeclaredClassFilter: GenerateClassFilter = GenerateClassFilter.GENERATE_ALL
        fun generateDeclaredClassFilter(v: GenerateClassFilter) =
            apply { generateDeclaredClassFilter = v }

        private var targetId: TargetId? = null
        fun targetId(v: TargetId?) =
            apply { targetId = v }

        private var moduleName: String? = configuration[CommonConfigurationKeys.MODULE_NAME]
        fun moduleName(v: String?) =
            apply { moduleName = v }

        // 'outDirectory' is a hack to correctly determine if a compiled class is from the same module as the callee during
        // partial compilation. Module chunks are treated as a single module.
        // TODO: get rid of it with the proper module infrastructure
        private var outDirectory: File? = null

        fun outDirectory(v: File?) =
            apply { outDirectory = v }

        private var onIndependentPartCompilationEnd: GenerationStateEventCallback = GenerationStateEventCallback.DO_NOTHING
        fun onIndependentPartCompilationEnd(v: GenerationStateEventCallback) =
            apply { onIndependentPartCompilationEnd = v }

        private var jvmBackendClassResolver: JvmBackendClassResolver = JvmBackendClassResolverForModuleWithDependencies(module)
        fun jvmBackendClassResolver(v: JvmBackendClassResolver) =
            apply { jvmBackendClassResolver = v }

        var ignoreErrors: Boolean = false
        fun ignoreErrors(v: Boolean): Builder =
            apply { ignoreErrors = v }

        var diagnosticReporter: DiagnosticReporter? = null
        fun diagnosticReporter(v: DiagnosticReporter) =
            apply { diagnosticReporter = v }

        fun build(): GenerationState {
            return GenerationState(
                project, builderFactory, module, configuration,
                generateDeclaredClassFilter, targetId,
                moduleName, outDirectory, onIndependentPartCompilationEnd,
                jvmBackendClassResolver, ignoreErrors,
                diagnosticReporter ?: DiagnosticReporterFactory.createReporter(configuration.messageCollector),
            )
        }
    }

    abstract class GenerateClassFilter {
        abstract fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGeneratePackagePart(ktFile: KtFile): Boolean

        companion object {
            @JvmField
            val GENERATE_ALL: GenerateClassFilter = object : GenerateClassFilter() {
                override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean = true
                override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean = true
            }
        }
    }

    val config = JvmBackendConfig(configuration)
    val languageVersionSettings = config.languageVersionSettings

    val inlineCache: InlineCache = InlineCache()

    val incrementalCacheForThisTarget: IncrementalCache?
    val deserializationConfiguration: DeserializationConfiguration =
        CompilerDeserializationConfiguration(languageVersionSettings)

    val deprecationProvider = DeprecationResolver(
        LockBasedStorageManager.NO_LOCKS, languageVersionSettings, JavaDeprecationSettings
    )

    init {
        val icComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        if (icComponents != null) {
            val targetId = targetId
                ?: moduleName?.let {
                    // hack for Gradle IC, Gradle does not use build.xml file, so there is no way to pass target id
                    TargetId(it, "java-production")
                } ?: error("Target ID should be specified for incremental compilation")
            incrementalCacheForThisTarget = icComponents.getIncrementalCache(targetId)
        } else {
            incrementalCacheForThisTarget = null
        }
    }

    private val interceptedBuilderFactory: ClassBuilderFactory
    private var used = false

    val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode
    val bindingTrace: BindingTrace = DelegatingBindingTrace(BindingContext.EMPTY, "trace in GenerationState")
    val localDelegatedProperties: MutableMap<Type, List<VariableDescriptorWithAccessors>> = mutableMapOf()

    val globalInlineContext: GlobalInlineContext = GlobalInlineContext()
    val factory: ClassFileFactory

    val scriptSpecific = ForScript()

    // TODO: review usages and consider replace mutability with explicit passing of input and output
    class ForScript {
        var resultFieldName: String? = null
        var resultType: KotlinType? = null
    }

    val globalSerializationBindings = JvmSerializationBindings()
    lateinit var mapInlineClass: (ClassDescriptor) -> Type

    class MultiFieldValueClassUnboxInfo(val unboxedTypesAndMethodNamesAndFieldNames: List<Triple<Type, String, String>>) {
        val unboxedTypes = unboxedTypesAndMethodNamesAndFieldNames.map { (type, _, _) -> type }
        val unboxedMethodNames = unboxedTypesAndMethodNamesAndFieldNames.map { (_, methodName, _) -> methodName }
    }

    var multiFieldValueClassUnboxInfo: (ClassDescriptor) -> MultiFieldValueClassUnboxInfo? = { null }

    lateinit var reportDuplicateClassNameError: (JvmDeclarationOrigin, String, String) -> Unit

    val typeApproximator: TypeApproximator? =
        if (languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
            TypeApproximator(module.builtIns, languageVersionSettings)
        else
            null

    init {
        this.interceptedBuilderFactory = builderFactory
            .wrapWith(
                {
                    if (classBuilderMode.generateBodies)
                        OptimizationClassBuilderFactory(it, this)
                    else
                        it
                },
                { BuilderFactoryForDuplicateClassNameDiagnostics(it, this) },
            )
            .wrapWith(loadClassBuilderInterceptors()) { classBuilderFactory, extension ->
                extension.interceptClassBuilderFactory(classBuilderFactory, BindingContext.EMPTY, DiagnosticSink.DO_NOTHING)
            }

        val finalizers = ClassFileFactoryFinalizerExtension.getInstances(project)
        this.factory = ClassFileFactory(this, interceptedBuilderFactory, finalizers)
    }

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

    fun beforeCompile() {
        markUsed()
    }

    fun afterIndependentPart() {
        onIndependentPartCompilationEnd(this)
    }

    private fun markUsed() {
        if (used) throw IllegalStateException("${GenerationState::class.java} cannot be used more than once")

        used = true
    }

    fun destroy() {
        interceptedBuilderFactory.close()
    }

    val newFragmentCaptureParameters: MutableList<Triple<String, KotlinType, DeclarationDescriptor>> = mutableListOf()
    fun recordNewFragmentCaptureParameter(string: String, type: KotlinType, descriptor: DeclarationDescriptor) {
        newFragmentCaptureParameters.add(Triple(string, type, descriptor))
    }
}

interface GenerationStateEventCallback : (GenerationState) -> Unit {
    companion object {
        val DO_NOTHING = GenerationStateEventCallback { }
    }
}

fun GenerationStateEventCallback(block: (GenerationState) -> Unit): GenerationStateEventCallback =
    object : GenerationStateEventCallback {
        override fun invoke(s: GenerationState) = block(s)
    }

private fun ClassBuilderFactory.wrapWith(vararg wrappers: (ClassBuilderFactory) -> ClassBuilderFactory): ClassBuilderFactory =
    wrappers.fold(this) { builderFactory, wrapper -> wrapper(builderFactory) }

private inline fun <T> ClassBuilderFactory.wrapWith(
    elements: Iterable<T>,
    wrapper: (ClassBuilderFactory, T) -> ClassBuilderFactory
): ClassBuilderFactory =
    elements.fold(this, wrapper)
