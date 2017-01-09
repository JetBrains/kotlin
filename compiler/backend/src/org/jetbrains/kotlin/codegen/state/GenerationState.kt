/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.`when`.MappingsClassesForWhenByEnum
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.RootContext
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceFilter
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import java.io.File

class GenerationState @JvmOverloads constructor(
        val project: Project,
        builderFactory: ClassBuilderFactory,
        val module: ModuleDescriptor,
        bindingContext: BindingContext,
        val files: List<KtFile>,
        disableCallAssertions: Boolean = true,
        disableParamAssertions: Boolean = true,
        val generateDeclaredClassFilter: GenerateClassFilter = GenerationState.GenerateClassFilter.GENERATE_ALL,
        disableInline: Boolean = false,
        disableOptimization: Boolean = false,
        val useTypeTableInSerializer: Boolean = false,
        val inheritMultifileParts: Boolean = false,
        val packagesWithObsoleteParts: Collection<FqName> = emptySet(),
        val obsoleteMultifileClasses: Collection<FqName> = emptySet(),
        // for PackageCodegen in incremental compilation mode
        val targetId: TargetId? = null,
        moduleName: String? = null,
        // 'outDirectory' is a hack to correctly determine if a compiled class is from the same module as the callee during
        // partial compilation. Module chunks are treated as a single module.
        // TODO: get rid of it with the proper module infrastructure
        val outDirectory: File? = null,
        val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        val progress: Progress = Progress.DEAF,
        private val onIndependentPartCompilationEnd: GenerationStateEventCallback = GenerationStateEventCallback.DO_NOTHING,
        dumpBinarySignatureMappingTo: File? = null,
        wantsDiagnostics: Boolean = true
) {
    abstract class GenerateClassFilter {
        abstract fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean
        abstract fun shouldGeneratePackagePart(jetFile: KtFile): Boolean
        abstract fun shouldGenerateScript(script: KtScript): Boolean

        companion object {
            @JvmField val GENERATE_ALL: GenerateClassFilter = object : GenerateClassFilter() {
                override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean = true

                override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean = true

                override fun shouldGenerateScript(script: KtScript): Boolean = true

                override fun shouldGeneratePackagePart(jetFile: KtFile): Boolean = true
            }
        }
    }

    val fileClassesProvider: CodegenFileClassesProvider = CodegenFileClassesProvider()
    val inlineCache: InlineCache = InlineCache()

    private fun getIncrementalCacheForThisTarget() =
            if (incrementalCompilationComponents != null && targetId != null)
                incrementalCompilationComponents.getIncrementalCache(targetId)
            else null

    val extraJvmDiagnosticsTrace: BindingTrace = DelegatingBindingTrace(bindingContext, "For extra diagnostics in ${this.javaClass}", false)
    private val interceptedBuilderFactory: ClassBuilderFactory
    private var used = false

    val diagnostics: DiagnosticSink get() = extraJvmDiagnosticsTrace
    val collectedExtraJvmDiagnostics: Diagnostics = LazyJvmDiagnostics {
        duplicateSignatureFactory.reportDiagnostics()
        extraJvmDiagnosticsTrace.bindingContext.diagnostics
    }

    val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    val classBuilderMode: ClassBuilderMode = builderFactory.classBuilderMode
    val bindingTrace: BindingTrace = DelegatingBindingTrace(bindingContext, "trace in GenerationState",
                                                            filter = if (wantsDiagnostics) BindingTraceFilter.ACCEPT_ALL else BindingTraceFilter.NO_DIAGNOSTICS)
    val bindingContext: BindingContext = bindingTrace.bindingContext
    val typeMapper: KotlinTypeMapper = KotlinTypeMapper(
            this.bindingContext, classBuilderMode, fileClassesProvider, IncompatibleClassTrackerImpl(extraJvmDiagnosticsTrace),
            this.moduleName
    )
    val intrinsics: IntrinsicMethods = IntrinsicMethods()
    val samWrapperClasses: SamWrapperClasses = SamWrapperClasses(this)
    val inlineCycleReporter: InlineCycleReporter = InlineCycleReporter(diagnostics)
    val mappingsClassesForWhenByEnum: MappingsClassesForWhenByEnum = MappingsClassesForWhenByEnum(this)
    val reflectionTypes: ReflectionTypes = ReflectionTypes(module)
    val jvmRuntimeTypes: JvmRuntimeTypes = JvmRuntimeTypes()
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

    val isCallAssertionsEnabled: Boolean = !disableCallAssertions
        @JvmName("isCallAssertionsEnabled") get

    val isParamAssertionsEnabled: Boolean = !disableParamAssertions
        @JvmName("isParamAssertionsEnabled") get

    val isInlineEnabled: Boolean = !disableInline
        @JvmName("isInlineEnabled") get


    val rootContext: CodegenContext<*> = RootContext(this)
    val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

    init {
        this.interceptedBuilderFactory = builderFactory
                .wrapWith(
                    { OptimizationClassBuilderFactory(it, disableOptimization) },
                    { BuilderFactoryForDuplicateSignatureDiagnostics(
                            it, this.bindingContext, diagnostics, fileClassesProvider, this.moduleName
                      ).apply { duplicateSignatureFactory = this } },
                    { BuilderFactoryForDuplicateClassNameDiagnostics(it, diagnostics) },
                    { dumpBinarySignatureMappingTo?.let { destination -> SignatureDumpingBuilderFactory(it, destination) } ?: it }
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
}

private class LazyJvmDiagnostics(compute: () -> Diagnostics): Diagnostics {
    private val delegate by lazy(LazyThreadSafetyMode.SYNCHRONIZED, compute)

    override val modificationTracker: ModificationTracker
        get() = delegate.modificationTracker

    override fun all(): Collection<Diagnostic> = delegate.all()

    override fun forElement(psiElement: PsiElement)  = delegate.forElement(psiElement)

    override fun isEmpty() = delegate.isEmpty()

    override fun noSuppression() = delegate.noSuppression()

    override fun iterator() = delegate.iterator()
}

interface GenerationStateEventCallback : (GenerationState) -> Unit {
    companion object {
        val DO_NOTHING = GenerationStateEventCallback {  }
    }
}

fun GenerationStateEventCallback(block: (GenerationState) -> Unit): GenerationStateEventCallback =
        object : GenerationStateEventCallback {
            override fun invoke(s: GenerationState) = block(s)
        }

private fun ClassBuilderFactory.wrapWith(vararg wrappers: (ClassBuilderFactory) -> ClassBuilderFactory): ClassBuilderFactory =
        wrappers.fold(this) { builderFactory, wrapper -> wrapper(builderFactory) }

private inline fun <T> ClassBuilderFactory.wrapWith(elements: Iterable<T>, wrapper: (ClassBuilderFactory, T) -> ClassBuilderFactory): ClassBuilderFactory =
        elements.fold(this, wrapper)
