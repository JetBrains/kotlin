/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.RootContext
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.codegen.inline.GlobalInlineContext
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.`when`.MappingsClassesForWhenByEnum
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.java.components.JavaDeprecationSettings
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.diagnostics.OnDemandSuppressCache
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmCompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.org.objectweb.asm.Type
import java.io.File

class GenerationState private constructor(
    val project: Project,
    builderFactory: ClassBuilderFactory,
    val module: ModuleDescriptor,
    val originalFrontendBindingContext: BindingContext,
    val configuration: CompilerConfiguration,
    val generateDeclaredClassFilter: GenerateClassFilter,
    val targetId: TargetId?,
    moduleName: String?,
    val outDirectory: File?,
    private val onIndependentPartCompilationEnd: GenerationStateEventCallback,
    val jvmBackendClassResolver: JvmBackendClassResolver,
    val isIrBackend: Boolean,
    val ignoreErrors: Boolean,
    val diagnosticReporter: DiagnosticReporter,
    val isIncrementalCompilation: Boolean
) {
    class Builder(
        private val project: Project,
        private val builderFactory: ClassBuilderFactory,
        private val module: ModuleDescriptor,
        private val bindingContext: BindingContext,
        private val configuration: CompilerConfiguration
    ) {
        // TODO: patch IntelliJ project and remove this compatibility c-tor
        constructor(
            project: Project,
            builderFactory: ClassBuilderFactory,
            module: ModuleDescriptor,
            bindingContext: BindingContext,
            files: List<KtFile>,
            configuration: CompilerConfiguration
        ) : this(project, builderFactory, module, bindingContext, configuration) {
            this.files = files
        }

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

        var isIrBackend: Boolean = configuration.getBoolean(JVMConfigurationKeys.IR)
        fun isIrBackend(v: Boolean) =
            apply { isIrBackend = v }

        var ignoreErrors: Boolean = false
        fun ignoreErrors(v: Boolean): Builder =
            apply { ignoreErrors = v }

        var diagnosticReporter: DiagnosticReporter? = null
        fun diagnosticReporter(v: DiagnosticReporter) =
            apply { diagnosticReporter = v }

        val isIncrementalCompilation: Boolean = configuration.getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)

        // TODO: remove after cleanin up IDE counterpart
        private var files: List<KtFile>? = null
        private var codegenFactory: CodegenFactory? = null
        fun codegenFactory(v: CodegenFactory) =
            apply { codegenFactory = v }

        fun build() =
            GenerationState(
                project, builderFactory, module, bindingContext, configuration,
                generateDeclaredClassFilter, targetId,
                moduleName, outDirectory, onIndependentPartCompilationEnd,
                jvmBackendClassResolver, isIrBackend, ignoreErrors,
                diagnosticReporter ?: DiagnosticReporterFactory.createReporter(),
                isIncrementalCompilation
            ).also {
                it.files = files
                it.codegenFactory = codegenFactory
            }
    }

    abstract class GenerateClassFilter {
        abstract fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGeneratePackagePart(ktFile: KtFile): Boolean
        abstract fun shouldGenerateScript(script: KtScript): Boolean
        abstract fun shouldGenerateCodeFragment(script: KtCodeFragment): Boolean
        open fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject) = shouldGenerateClass(processingClassOrObject)

        companion object {
            @JvmField
            val GENERATE_ALL: GenerateClassFilter = object : GenerateClassFilter() {
                override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean = true
                override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean = true
                override fun shouldGenerateScript(script: KtScript): Boolean = true
                override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean = true
                override fun shouldGenerateCodeFragment(script: KtCodeFragment) = true
            }
        }
    }

    val config = JvmBackendConfig(configuration)
    val languageVersionSettings = config.languageVersionSettings

    val inlineCache: InlineCache = InlineCache()

    val incrementalCacheForThisTarget: IncrementalCache?
    val packagesWithObsoleteParts: Set<FqName>
    val obsoleteMultifileClasses: List<FqName>
    val deserializationConfiguration: DeserializationConfiguration =
        JvmCompilerDeserializationConfiguration(languageVersionSettings)

    val deprecationProvider = DeprecationResolver(
        LockBasedStorageManager.NO_LOCKS, languageVersionSettings, JavaDeprecationSettings
    )

    // TODO: remove after cleanin up IDE counterpart
    var files: List<KtFile>? = null
    var codegenFactory: CodegenFactory? = null

    init {
        val icComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        if (icComponents != null) {
            val targetId = targetId
                ?: moduleName?.let {
                    // hack for Gradle IC, Gradle does not use build.xml file, so there is no way to pass target id
                    TargetId(it, "java-production")
                } ?: error("Target ID should be specified for incremental compilation")
            incrementalCacheForThisTarget = icComponents.getIncrementalCache(targetId)
            packagesWithObsoleteParts = incrementalCacheForThisTarget.getObsoletePackageParts().map {
                JvmClassName.byInternalName(it).packageFqName
            }.toSet()
            obsoleteMultifileClasses = incrementalCacheForThisTarget.getObsoleteMultifileClasses().map {
                JvmClassName.byInternalName(it).fqNameForClassNameWithoutDollars
            }
        } else {
            incrementalCacheForThisTarget = null
            packagesWithObsoleteParts = emptySet()
            obsoleteMultifileClasses = emptyList()
        }
    }

    private val extraJvmDiagnosticsTrace: BindingTrace =
        DelegatingBindingTrace(
            originalFrontendBindingContext, "For extra diagnostics in ${this::class.java}", false,
            customSuppressCache = if (isIrBackend) OnDemandSuppressCache(originalFrontendBindingContext) else null,
        )

    private val interceptedBuilderFactory: ClassBuilderFactory
    private var used = false

    val diagnostics: DiagnosticSink get() = extraJvmDiagnosticsTrace
    val collectedExtraJvmDiagnostics: Diagnostics = LazyJvmDiagnostics {
        duplicateSignatureFactory?.reportDiagnostics()
        extraJvmDiagnosticsTrace.bindingContext.diagnostics
    }

    val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode
    val bindingTrace: BindingTrace = DelegatingBindingTrace(originalFrontendBindingContext, "trace in GenerationState")
    val bindingContext: BindingContext = bindingTrace.bindingContext
    val mainFunctionDetector = MainFunctionDetector(originalFrontendBindingContext, languageVersionSettings)
    val typeMapper: KotlinTypeMapper = KotlinTypeMapper(
        bindingContext,
        classBuilderMode,
        this.moduleName,
        languageVersionSettings,
        config.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures,
        config.target,
        isIrBackend
    )
    val intrinsics: IntrinsicMethods =
        IntrinsicMethods(languageVersionSettings.apiVersion <= ApiVersion.parse(KotlinVersion.CURRENT.toString())!!)

    val samWrapperClasses: SamWrapperClasses = SamWrapperClasses(this)
    val globalInlineContext: GlobalInlineContext = GlobalInlineContext()
    val mappingsClassesForWhenByEnum: MappingsClassesForWhenByEnum = MappingsClassesForWhenByEnum(this)
    val jvmRuntimeTypes: JvmRuntimeTypes = JvmRuntimeTypes(
        module, languageVersionSettings, config.generateOptimizedCallableReferenceSuperClasses
    )
    val factory: ClassFileFactory
    private var duplicateSignatureFactory: BuilderFactoryForDuplicateSignatureDiagnostics? = null

    val scriptSpecific = ForScript()

    // TODO: review usages and consider replace mutability with explicit passing of input and output
    class ForScript {
        // quite a mess, this one is an input from repl interpreter
        var earlierScriptsForReplInterpreter: List<ScriptDescriptor>? = null

        // and the rest is an output from the codegen
        var resultFieldName: String? = null
        var resultType: KotlinType? = null
    }

    val rootContext: CodegenContext<*> = RootContext(this)

    val jvmDefaultMode: JvmDefaultMode
        get() = config.jvmDefaultMode

    val globalSerializationBindings = JvmSerializationBindings()
    var mapInlineClass: (ClassDescriptor) -> Type = { descriptor -> typeMapper.mapType(descriptor.defaultType) }

    class MultiFieldValueClassUnboxInfo(val unboxedTypesAndMethodNamesAndFieldNames: List<Triple<Type, String, String>>) {
        val unboxedTypes = unboxedTypesAndMethodNamesAndFieldNames.map { (type, _, _) -> type }
        val unboxedMethodNames = unboxedTypesAndMethodNamesAndFieldNames.map { (_, methodName, _) -> methodName }
        val unboxedFieldNames = unboxedTypesAndMethodNamesAndFieldNames.map { (_, _, fieldName) -> fieldName }
    }

    var multiFieldValueClassUnboxInfo: (ClassDescriptor) -> MultiFieldValueClassUnboxInfo? = { null }

    var reportDuplicateClassNameError: (JvmDeclarationOrigin, String, String) -> Unit = { origin, internalName, duplicateClasses ->
        origin.element?.let {
            diagnostics.report(ErrorsJvm.DUPLICATE_CLASS_NAMES.on(it, internalName, duplicateClasses))
        }
    }

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
                {
                    // In IR backend, we have more precise information about classes and methods we are going to generate,
                    // and report signature conflict errors in JvmSignatureClashDetector.
                    if (isIrBackend)
                        it
                    else
                        BuilderFactoryForDuplicateSignatureDiagnostics(
                            it, bindingContext, diagnostics, this.moduleName, languageVersionSettings,
                            config.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures,
                            shouldGenerate = { origin -> !shouldOnlyCollectSignatures(origin) },
                        ).apply { duplicateSignatureFactory = this }
                },
                { BuilderFactoryForDuplicateClassNameDiagnostics(it, this) },
            )
            .wrapWith(loadClassBuilderInterceptors()) { classBuilderFactory, extension ->
                extension.interceptClassBuilderFactory(classBuilderFactory, originalFrontendBindingContext, diagnostics)
            }

        val finalizers = ClassFileFactoryFinalizerExtension.getInstances(project)
        this.factory = ClassFileFactory(this, interceptedBuilderFactory, finalizers)
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION_ERROR")
    private fun loadClassBuilderInterceptors(): List<org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension> {
        // Using Class.forName here because we're in the old JVM backend, and we need to load extensions declared in the JVM IR backend.
        val adapted = Class.forName("org.jetbrains.kotlin.backend.jvm.extensions.ClassBuilderExtensionAdapter")
            .getDeclaredMethod("getExtensions", Project::class.java)
            .invoke(null, project) as List<org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension>

        return org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension.getInstances(project) + adapted
    }

    fun beforeCompile() {
        markUsed()
    }

    fun oldBEInitTrace(ktFiles: Collection<KtFile>) {
        if (!isIrBackend) {
            CodegenBinding.initTrace(this, ktFiles)
        }
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

    private fun shouldOnlyCollectSignatures(origin: JvmDeclarationOrigin) =
        classBuilderMode == ClassBuilderMode.LIGHT_CLASSES && origin.originKind in doNotGenerateInLightClassMode

    val newFragmentCaptureParameters: MutableList<Triple<String, KotlinType, DeclarationDescriptor>> = mutableListOf()
    fun recordNewFragmentCaptureParameter(string: String, type: KotlinType, descriptor: DeclarationDescriptor) {
        newFragmentCaptureParameters.add(Triple(string, type, descriptor))
    }
}

private val doNotGenerateInLightClassMode = setOf(CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL, BRIDGE, COLLECTION_STUB, AUGMENTED_BUILTIN_API)

private class LazyJvmDiagnostics(compute: () -> Diagnostics) : Diagnostics {
    private val delegate by lazy(LazyThreadSafetyMode.SYNCHRONIZED, compute)

    override val modificationTracker: ModificationTracker
        get() = delegate.modificationTracker

    override fun all(): Collection<Diagnostic> = delegate.all()

    override fun forElement(psiElement: PsiElement) = delegate.forElement(psiElement)

    override fun isEmpty() = delegate.isEmpty()

    override fun noSuppression() = delegate.noSuppression()

    override fun iterator() = delegate.iterator()
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
