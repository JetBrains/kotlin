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
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.`when`.MappingsClassesForWhenByEnum
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.RootContext
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import java.io.File

public class GenerationState @JvmOverloads constructor(
        public val project: Project,
        builderFactory: ClassBuilderFactory,
        public val module: ModuleDescriptor,
        bindingContext: BindingContext,
        public val files: List<JetFile>,
        disableCallAssertions: Boolean = true,
        disableParamAssertions: Boolean = true,
        public val generateDeclaredClassFilter: GenerationState.GenerateClassFilter = GenerationState.GenerateClassFilter.GENERATE_ALL,
        disableInline: Boolean = false,
        disableOptimization: Boolean = false,
        public val packageFacadesAsMultifileClasses: Boolean = false,
        public val diagnostics: DiagnosticSink = DiagnosticSink.DO_NOTHING,
        public val packagesWithObsoleteParts: Collection<FqName> = emptySet(),
        public val obsoleteMultifileClasses: Collection<FqName> = emptySet(),
        // for PackageCodegen in incremental compilation mode
        public val targetId: TargetId? = null,
        moduleName: String? = null,
        // 'outDirectory' is a hack to correctly determine if a compiled class is from the same module as the callee during
        // partial compilation. Module chunks are treated as a single module.
        // TODO: get rid of it with the proper module infrastructure
        public val outDirectory: File? = null,
        public val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        public val progress: Progress = Progress.DEAF
) {
    public interface GenerateClassFilter {
        public fun shouldAnnotateClass(classOrObject: JetClassOrObject): Boolean
        public fun shouldGenerateClass(classOrObject: JetClassOrObject): Boolean
        public fun shouldGeneratePackagePart(jetFile: JetFile): Boolean
        public fun shouldGenerateScript(script: JetScript): Boolean

        companion object {
            public val GENERATE_ALL: GenerateClassFilter = object : GenerateClassFilter {
                override fun shouldAnnotateClass(classOrObject: JetClassOrObject): Boolean = true

                override fun shouldGenerateClass(classOrObject: JetClassOrObject): Boolean = true

                override fun shouldGenerateScript(script: JetScript): Boolean = true

                override fun shouldGeneratePackagePart(jetFile: JetFile): Boolean = true
            }
        }
    }

    public val fileClassesProvider: CodegenFileClassesProvider = CodegenFileClassesProvider(packageFacadesAsMultifileClasses)

    private fun getIncrementalCacheForThisTarget() =
            if (incrementalCompilationComponents != null && targetId != null)
                incrementalCompilationComponents.getIncrementalCache(targetId)
            else null

    public val moduleName: String = moduleName ?: JvmCodegenUtil.getModuleName(module)
    public val classBuilderMode: ClassBuilderMode = builderFactory.getClassBuilderMode()
    public val bindingTrace: BindingTrace = DelegatingBindingTrace(bindingContext, "trace in GenerationState")
    public val bindingContext: BindingContext = bindingTrace.getBindingContext()
    public val typeMapper: JetTypeMapper = JetTypeMapper(this.bindingContext, classBuilderMode, fileClassesProvider, getIncrementalCacheForThisTarget(), this.moduleName)
    public val intrinsics: IntrinsicMethods = IntrinsicMethods()
    public val samWrapperClasses: SamWrapperClasses = SamWrapperClasses(this)
    public val inlineCycleReporter: InlineCycleReporter = InlineCycleReporter(diagnostics)
    public val mappingsClassesForWhenByEnum: MappingsClassesForWhenByEnum = MappingsClassesForWhenByEnum(this)
    public var earlierScriptsForReplInterpreter: List<ScriptDescriptor>? = null
    public val reflectionTypes: ReflectionTypes = ReflectionTypes(module)
    public val jvmRuntimeTypes: JvmRuntimeTypes = JvmRuntimeTypes()
    public val factory: ClassFileFactory
    private val interceptedBuilderFactory: ClassBuilderFactory
    private var used = false

    public val isCallAssertionsEnabled: Boolean = !disableCallAssertions
        @JvmName("isCallAssertionsEnabled") get

    public val isParamAssertionsEnabled: Boolean = !disableParamAssertions
        @JvmName("isParamAssertionsEnabled") get

    public val isInlineEnabled: Boolean = !disableInline
        @JvmName("isInlineEnabled") get


    public val rootContext: CodegenContext<*> = RootContext(this)

    init {
        val optimizationClassBuilderFactory = OptimizationClassBuilderFactory(builderFactory, disableOptimization)
        var interceptedBuilderFactory: ClassBuilderFactory = BuilderFactoryForDuplicateSignatureDiagnostics(
                optimizationClassBuilderFactory, this.bindingContext, diagnostics, fileClassesProvider,
                getIncrementalCacheForThisTarget(),
                this.moduleName)

        interceptedBuilderFactory = BuilderFactoryForDuplicateClassNameDiagnostics(interceptedBuilderFactory, diagnostics);

        val interceptExtensions = ClassBuilderInterceptorExtension.getInstances(project)

        for (extension in interceptExtensions) {
            interceptedBuilderFactory = extension.interceptClassBuilderFactory(interceptedBuilderFactory, bindingContext, diagnostics)
        }

        this.interceptedBuilderFactory = interceptedBuilderFactory
        this.factory = ClassFileFactory(this, interceptedBuilderFactory)
    }

    public fun beforeCompile() {
        markUsed()

        CodegenBinding.initTrace(this)
    }

    private fun markUsed() {
        if (used) throw IllegalStateException("${GenerationState::class.java} cannot be used more than once")

        used = true
    }

    public fun destroy() {
        interceptedBuilderFactory.close()
    }
}
