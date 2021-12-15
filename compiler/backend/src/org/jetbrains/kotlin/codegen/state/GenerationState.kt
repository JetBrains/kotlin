/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.`when`.MappingsClassesForWhenByEnum
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.RootContext
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.codegen.inline.GlobalInlineContext
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.LanguageVersion.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.java.components.JavaDeprecationSettings
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.diagnostics.OnDemandSuppressCache
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.org.objectweb.asm.Type
import java.io.File
import java.util.*

class GenerationState private constructor(
    val project: Project,
    builderFactory: ClassBuilderFactory,
    val module: ModuleDescriptor,
    val originalFrontendBindingContext: BindingContext,
    val files: List<KtFile>,
    val configuration: CompilerConfiguration,
    val generateDeclaredClassFilter: GenerateClassFilter,
    val codegenFactory: CodegenFactory,
    val targetId: TargetId?,
    moduleName: String?,
    val outDirectory: File?,
    private val onIndependentPartCompilationEnd: GenerationStateEventCallback,
    wantsDiagnostics: Boolean,
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
        private val files: List<KtFile>,
        private val configuration: CompilerConfiguration
    ) {
        private var generateDeclaredClassFilter: GenerateClassFilter = GenerateClassFilter.GENERATE_ALL
        fun generateDeclaredClassFilter(v: GenerateClassFilter) =
            apply { generateDeclaredClassFilter = v }

        private var codegenFactory: CodegenFactory = DefaultCodegenFactory
        fun codegenFactory(v: CodegenFactory) =
            apply { codegenFactory = v }

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

        private var wantsDiagnostics: Boolean = true
        fun wantsDiagnostics(v: Boolean) =
            apply { wantsDiagnostics = v }

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

        fun build() =
            GenerationState(
                project, builderFactory, module, bindingContext, files, configuration,
                generateDeclaredClassFilter, codegenFactory, targetId,
                moduleName, outDirectory, onIndependentPartCompilationEnd, wantsDiagnostics,
                jvmBackendClassResolver, isIrBackend, ignoreErrors,
                diagnosticReporter ?: DiagnosticReporterFactory.createReporter(),
                isIncrementalCompilation
            )
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

    val languageVersionSettings = configuration.languageVersionSettings

    val inlineCache: InlineCache = InlineCache()

    val incrementalCacheForThisTarget: IncrementalCache?
    val packagesWithObsoleteParts: Set<FqName>
    val obsoleteMultifileClasses: List<FqName>
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

    val extraJvmDiagnosticsTrace: BindingTrace =
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

    val useOldManglingSchemeForFunctionsWithInlineClassesInSignatures =
        configuration.getBoolean(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME) ||
                languageVersionSettings.languageVersion.run { major == 1 && minor < 4 }

    val target = configuration.get(JVMConfigurationKeys.JVM_TARGET) ?: JvmTarget.DEFAULT
    val runtimeStringConcat =
        if (target.majorVersion >= JvmTarget.JVM_9.majorVersion)
            configuration.get(JVMConfigurationKeys.STRING_CONCAT) ?: JvmStringConcat.INDY_WITH_CONSTANTS
        else JvmStringConcat.INLINE

    val samConversionsScheme = run {
        val fromConfig = configuration.get(JVMConfigurationKeys.SAM_CONVERSIONS)
        if (fromConfig != null && target >= fromConfig.minJvmTarget)
            fromConfig
        else if (
            target >= JvmClosureGenerationScheme.INDY.minJvmTarget &&
            languageVersionSettings.supportsFeature(LanguageFeature.SamWrapperClassesAreSynthetic)
        )
            JvmClosureGenerationScheme.INDY
        else
            JvmClosureGenerationScheme.CLASS
    }

    val lambdasScheme = run {
        val fromConfig = configuration.get(JVMConfigurationKeys.LAMBDAS)
            ?: JvmClosureGenerationScheme.DEFAULT
        if (target >= fromConfig.minJvmTarget)
            fromConfig
        else
            JvmClosureGenerationScheme.DEFAULT
    }

    val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode
    val bindingTrace: BindingTrace = DelegatingBindingTrace(
        originalFrontendBindingContext, "trace in GenerationState",
        filter = if (wantsDiagnostics) BindingTraceFilter.ACCEPT_ALL else BindingTraceFilter.NO_DIAGNOSTICS
    )
    val bindingContext: BindingContext = bindingTrace.bindingContext
    val mainFunctionDetector = MainFunctionDetector(originalFrontendBindingContext, languageVersionSettings)
    val typeMapper: KotlinTypeMapper = KotlinTypeMapper(
        bindingContext,
        classBuilderMode,
        this.moduleName,
        languageVersionSettings,
        useOldManglingSchemeForFunctionsWithInlineClassesInSignatures,
        target,
        isIrBackend
    )
    val canReplaceStdlibRuntimeApiBehavior = languageVersionSettings.apiVersion <= ApiVersion.parse(KotlinVersion.CURRENT.toString())!!
    val intrinsics: IntrinsicMethods = IntrinsicMethods(target, canReplaceStdlibRuntimeApiBehavior)
    val generateOptimizedCallableReferenceSuperClasses =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_OPTIMIZED_CALLABLE_REFERENCES)
    val useKotlinNothingValueException =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_KOTLIN_NOTHING_VALUE_EXCEPTION)

    // In 1.6, `typeOf` became stable and started to rely on a few internal stdlib functions which were missing before 1.6.
    val stableTypeOf = languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_6

    val samWrapperClasses: SamWrapperClasses = SamWrapperClasses(this)
    val globalInlineContext: GlobalInlineContext = GlobalInlineContext(diagnostics)
    val mappingsClassesForWhenByEnum: MappingsClassesForWhenByEnum = MappingsClassesForWhenByEnum(this)
    val jvmRuntimeTypes: JvmRuntimeTypes = JvmRuntimeTypes(
        module, languageVersionSettings, generateOptimizedCallableReferenceSuperClasses
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
        var resultTypeString: String? = null
        var resultType: KotlinType? = null
    }

    val isCallAssertionsDisabled: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS)
    val isReceiverAssertionsDisabled: Boolean =
        configuration.getBoolean(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS) ||
                !languageVersionSettings.supportsFeature(LanguageFeature.NullabilityAssertionOnExtensionReceiver)
    val isParamAssertionsDisabled: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS)
    val assertionsMode: JVMAssertionsMode = configuration.get(JVMConfigurationKeys.ASSERTIONS_MODE, JVMAssertionsMode.DEFAULT)
    val isInlineDisabled: Boolean = configuration.getBoolean(CommonConfigurationKeys.DISABLE_INLINE)
    val useTypeTableInSerializer: Boolean = configuration.getBoolean(JVMConfigurationKeys.USE_TYPE_TABLE)
    val unifiedNullChecks: Boolean =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS)
    val functionsWithInlineClassReturnTypesMangled: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.MangleClassMembersReturningInlineClasses)
    val shouldValidateIr = configuration.getBoolean(JVMConfigurationKeys.VALIDATE_IR)
    val shouldValidateBytecode = configuration.getBoolean(JVMConfigurationKeys.VALIDATE_BYTECODE)

    val rootContext: CodegenContext<*> = RootContext(this)

    val classFileVersion: Int = run {
        val minorVersion = if (configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)) 0xffff else 0
        (minorVersion shl 16) + target.majorVersion
    }

    val generateParametersMetadata: Boolean = configuration.getBoolean(JVMConfigurationKeys.PARAMETERS_METADATA)

    val shouldInlineConstVals = languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)

    val jvmDefaultMode = languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)

    val disableOptimization = configuration.get(JVMConfigurationKeys.DISABLE_OPTIMIZATION, false)

    val metadataVersion =
        configuration.get(CommonConfigurationKeys.METADATA_VERSION)
            ?: LANGUAGE_TO_METADATA_VERSION.getValue(languageVersionSettings.languageVersion)

    val abiStability = configuration.get(JVMConfigurationKeys.ABI_STABILITY)

    val globalSerializationBindings = JvmSerializationBindings()
    var mapInlineClass: (ClassDescriptor) -> Type = { descriptor -> typeMapper.mapType(descriptor.defaultType) }

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
                    // and report signature conflict errors in JvmSignatureClashTracker.
                    if (isIrBackend)
                        it
                    else
                        BuilderFactoryForDuplicateSignatureDiagnostics(
                            it, bindingContext, diagnostics, this.moduleName, languageVersionSettings,
                            useOldManglingSchemeForFunctionsWithInlineClassesInSignatures,
                            shouldGenerate = { origin -> !shouldOnlyCollectSignatures(origin) },
                        ).apply { duplicateSignatureFactory = this }
                },
                { BuilderFactoryForDuplicateClassNameDiagnostics(it, diagnostics) },
                {
                    configuration.get(JVMConfigurationKeys.DECLARATIONS_JSON_PATH)
                        ?.let { destination -> SignatureDumpingBuilderFactory(it, File(destination)) } ?: it
                }
            )
            .wrapWith(ClassBuilderInterceptorExtension.getInstances(project)) { classBuilderFactory, extension ->
                extension.interceptClassBuilderFactory(classBuilderFactory, originalFrontendBindingContext, diagnostics)
            }

        val finalizers = ClassFileFactoryFinalizerExtension.getInstances(project)
        this.factory = ClassFileFactory(this, interceptedBuilderFactory, finalizers)
    }

    fun beforeCompile() {
        markUsed()

        if (!isIrBackend) {
            CodegenBinding.initTrace(this)
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

    companion object {
        val LANGUAGE_TO_METADATA_VERSION = EnumMap<LanguageVersion, JvmMetadataVersion>(LanguageVersion::class.java).apply {
            val oldMetadataVersion = JvmMetadataVersion(1, 1, 18)
            this[KOTLIN_1_0] = oldMetadataVersion
            this[KOTLIN_1_1] = oldMetadataVersion
            this[KOTLIN_1_2] = oldMetadataVersion
            this[KOTLIN_1_3] = oldMetadataVersion
            this[KOTLIN_1_4] = JvmMetadataVersion(1, 4, 3)
            this[KOTLIN_1_5] = JvmMetadataVersion(1, 5, 1)
            this[KOTLIN_1_6] = JvmMetadataVersion.INSTANCE
            this[KOTLIN_1_7] = JvmMetadataVersion(1, 7, 0)
            this[KOTLIN_1_8] = JvmMetadataVersion(1, 8, 0)
            this[KOTLIN_1_9] = JvmMetadataVersion(1, 9, 0)

            check(size == LanguageVersion.values().size) {
                "Please add mappings from the missing LanguageVersion instances to the corresponding JvmMetadataVersion " +
                        "in `GenerationState.LANGUAGE_TO_METADATA_VERSION`"
            }
        }
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
