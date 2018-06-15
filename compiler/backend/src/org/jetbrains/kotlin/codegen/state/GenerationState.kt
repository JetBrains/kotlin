/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.codegen.inline.GlobalInlineContext
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File

class GenerationState private constructor(
    val project: Project,
    builderFactory: ClassBuilderFactory,
    val module: ModuleDescriptor,
    bindingContext: BindingContext,
    val files: List<KtFile>,
    val configuration: CompilerConfiguration,
    val generateDeclaredClassFilter: GenerateClassFilter,
    val codegenFactory: CodegenFactory,
    val targetId: TargetId?,
    moduleName: String?,
    val outDirectory: File?,
    private val onIndependentPartCompilationEnd: GenerationStateEventCallback,
    wantsDiagnostics: Boolean
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

        fun build() =
            GenerationState(
                project, builderFactory, module, bindingContext, files, configuration,
                generateDeclaredClassFilter, codegenFactory, targetId,
                moduleName, outDirectory, onIndependentPartCompilationEnd, wantsDiagnostics
            )
    }

    abstract class GenerateClassFilter {
        abstract fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGeneratePackagePart(ktFile: KtFile): Boolean
        abstract fun shouldGenerateScript(script: KtScript): Boolean
        open fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject) = shouldGenerateClass(processingClassOrObject)

        companion object {
            @JvmField
            val GENERATE_ALL: GenerateClassFilter = object : GenerateClassFilter() {
                override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean = true

                override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean = true

                override fun shouldGenerateScript(script: KtScript): Boolean = true

                override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean = true
            }
        }
    }

    val inlineCache: InlineCache = InlineCache()

    val incrementalCacheForThisTarget: IncrementalCache?
    val packagesWithObsoleteParts: Set<FqName>
    val obsoleteMultifileClasses: List<FqName>
    val deserializationConfiguration: DeserializationConfiguration =
        CompilerDeserializationConfiguration(configuration.languageVersionSettings)

    val deprecationProvider = DeprecationResolver(LockBasedStorageManager.NO_LOCKS, configuration.languageVersionSettings)

    init {
        val icComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        if (icComponents != null) {
            incrementalCacheForThisTarget =
                    icComponents.getIncrementalCache(targetId ?: error("Target ID should be specified for incremental compilation"))
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
        DelegatingBindingTrace(bindingContext, "For extra diagnostics in ${this::class.java}", false)
    private val interceptedBuilderFactory: ClassBuilderFactory
    private var used = false

    val diagnostics: DiagnosticSink get() = extraJvmDiagnosticsTrace
    val collectedExtraJvmDiagnostics: Diagnostics = LazyJvmDiagnostics {
        duplicateSignatureFactory.reportDiagnostics()
        extraJvmDiagnosticsTrace.bindingContext.diagnostics
    }

    val languageVersionSettings = configuration.languageVersionSettings

    val target = configuration.get(JVMConfigurationKeys.JVM_TARGET) ?: JvmTarget.DEFAULT
    val isJvm8Target: Boolean = target == JvmTarget.JVM_1_8

    val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode
    val bindingTrace: BindingTrace = DelegatingBindingTrace(
        bindingContext, "trace in GenerationState",
        filter = if (wantsDiagnostics) BindingTraceFilter.ACCEPT_ALL else BindingTraceFilter.NO_DIAGNOSTICS
    )
    val bindingContext: BindingContext = bindingTrace.bindingContext
    val typeMapper: KotlinTypeMapper = KotlinTypeMapper(
        this.bindingContext,
        classBuilderMode,
        IncompatibleClassTrackerImpl(extraJvmDiagnosticsTrace),
        this.moduleName,
        isJvm8Target,
        configuration.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
    )
    val intrinsics: IntrinsicMethods = run {
        val shouldUseConsistentEquals = languageVersionSettings.supportsFeature(LanguageFeature.ThrowNpeOnExplicitEqualsForBoxedNull) &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_EXCEPTION_ON_EXPLICIT_EQUALS_FOR_BOXED_NULL)
        IntrinsicMethods(target, shouldUseConsistentEquals)
    }
    val samWrapperClasses: SamWrapperClasses = SamWrapperClasses(this)
    val globalInlineContext: GlobalInlineContext = GlobalInlineContext(diagnostics)
    val mappingsClassesForWhenByEnum: MappingsClassesForWhenByEnum = MappingsClassesForWhenByEnum(this)
    val jvmRuntimeTypes: JvmRuntimeTypes = JvmRuntimeTypes(module, configuration.languageVersionSettings)
    val factory: ClassFileFactory
    private lateinit var duplicateSignatureFactory: BuilderFactoryForDuplicateSignatureDiagnostics

    val replSpecific = ForRepl()

    //TODO: should be refactored out
    class ForRepl {
        var earlierScriptsForReplInterpreter: List<ScriptDescriptor>? = null
        var scriptResultFieldName: String? = null
        val shouldGenerateScriptResultValue: Boolean get() = scriptResultFieldName != null
        var hasResult: Boolean = false
    }

    val isCallAssertionsDisabled: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS)
    val isReceiverAssertionsDisabled: Boolean =
        configuration.getBoolean(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS) ||
                !languageVersionSettings.supportsFeature(LanguageFeature.NullabilityAssertionOnExtensionReceiver)
    val isParamAssertionsDisabled: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS)
    val assertionsMode: JVMAssertionsMode = configuration.get(JVMConfigurationKeys.ASSERTIONS_MODE, JVMAssertionsMode.DEFAULT)
    val isInlineDisabled: Boolean = configuration.getBoolean(CommonConfigurationKeys.DISABLE_INLINE)
    val useTypeTableInSerializer: Boolean = configuration.getBoolean(JVMConfigurationKeys.USE_TYPE_TABLE)
    val inheritMultifileParts: Boolean = configuration.getBoolean(JVMConfigurationKeys.INHERIT_MULTIFILE_PARTS)

    val rootContext: CodegenContext<*> = RootContext(this)

    val classFileVersion: Int = target.bytecodeVersion

    val generateParametersMetadata: Boolean = configuration.getBoolean(JVMConfigurationKeys.PARAMETERS_METADATA)

    val shouldInlineConstVals = languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)

    val constructorCallNormalizationMode = configuration.get(
        JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE,
        JVMConstructorCallNormalizationMode.DEFAULT
    )

    val jvmDefaultMode = languageVersionSettings.getFlag(AnalysisFlag.jvmDefaultMode)

    val disableOptimization = configuration.get(JVMConfigurationKeys.DISABLE_OPTIMIZATION, false)

    init {
        this.interceptedBuilderFactory = builderFactory
            .wrapWith(
                { OptimizationClassBuilderFactory(it, this) },
                {
                    BuilderFactoryForDuplicateSignatureDiagnostics(
                        it, this.bindingContext, diagnostics, this.moduleName,
                        isReleaseCoroutines = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines),
                        shouldGenerate = { !shouldOnlyCollectSignatures(it) }
                    ).apply { duplicateSignatureFactory = this }
                },
                { BuilderFactoryForDuplicateClassNameDiagnostics(it, diagnostics) },
                {
                    configuration.get(JVMConfigurationKeys.DECLARATIONS_JSON_PATH)
                        ?.let { destination -> SignatureDumpingBuilderFactory(it, File(destination)) } ?: it
                }
            )
            .wrapWith(ClassBuilderInterceptorExtension.getInstances(project)) { builderFactory, extension ->
                extension.interceptClassBuilderFactory(builderFactory, bindingContext, diagnostics)
            }

        this.factory = ClassFileFactory(this, interceptedBuilderFactory)
    }

    fun beforeCompile() {
        markUsed()

        CodegenBinding.initTrace(this)
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
