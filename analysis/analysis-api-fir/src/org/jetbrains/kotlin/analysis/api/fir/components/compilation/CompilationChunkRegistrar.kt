/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirCompilerFacility
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaDanglingFileModuleImpl
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.explicitModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPlatformActualizer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.yieldIfNotNull
import java.util.*

private val USE_STDLIB_BUILD_OUTPUT: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`("kotlin.analysis.compilerFacility.useStdlibBuildOutput", true)
}

/**
 * Configuration of a compilation chunk to be created.
 *
 * @param effectiveModule The module to which all files in the chunk either belong or have as a context.
 * @param kind Whether the chunk contains the main file (a file for which compilation was requested), its context or a dependency.
 * @param isDanglingChild Whether a new dangling module with the [effectiveModule] as a context module must be created, instead of reusing
 *   [effectiveModule] where possible.
 * @param attachPrecompiledBinaries Whether to attach compiled bytecode of the module instead of compiling the module files.
 */
private data class ChunkSpec(
    val originalModule: KaModule,
    val effectiveModule: KaModule,
    val kind: CompilationChunkKind,
    val isDanglingChild: Boolean,
    val attachPrecompiledBinaries: Boolean,
)

/**
 * A facility for splitting the list of input files into chunks.
 *
 * Here is how to use it:
 * 1. Call [submit], passing every file to compile (including the dependencies).
 * 2. Call [computeChunks] to get the resulting map of chunks.
 *
 * @param originalMainFile The unmodified [KtFile] for the main file.
 * @param originalMainFirFile The [FirFile] representing the [originalMainFile].
 * @param actualizer The provider of implementation counterparts for common modules.
 */
internal class CompilationChunkRegistrar(
    private val originalMainFile: KtFile,
    private val originalMainFirFile: FirFile,
    private val actualizer: LLPlatformActualizer?,
    private val project: Project,
    private val resolutionFacade: LLResolutionFacade,
) {
    private val originalMainModule = originalMainFirFile.llFirModuleData.ktModule
    private val originalMainContextModule = (originalMainModule as? KaDanglingFileModule)?.contextModule

    private val submittedChunks = LinkedHashMap<ChunkSpec, MutableSet<KtFile>>()

    /**
     * Attach the file to the appropriate chunk.
     * The [module] parameter is used for optimization, and it corresponds to [LLResolutionFacade.getModule] called on the [file].
     */
    fun submit(file: KtFile, module: KaModule) {
        val chunkKind = when (module) {
            originalMainModule -> CompilationChunkKind.MAIN
            originalMainContextModule -> CompilationChunkKind.CONTEXT
            else -> CompilationChunkKind.DEPENDENCY
        }

        val attachPrecompiledBinaries = shouldAttachPrecompiledBinaries(file)

        fun register(spec: ChunkSpec, file: KtFile, alwaysAttachFile: Boolean = false) {
            val chunkSpec = submittedChunks.getOrPut(spec, ::LinkedHashSet)

            // Even if precompiled binaries should be attached instead of source files, still attach dangling files.
            // Covered scenario: debugging the 'kotlin-stdlib' implementation.
            if (alwaysAttachFile || !attachPrecompiledBinaries) {
                chunkSpec.add(file)
            }
        }

        when {
            module is KaDanglingFileModule -> {
                if (module.isSupported) {
                    val spec = ChunkSpec(
                        originalModule = module,
                        effectiveModule = module,
                        kind = chunkKind,
                        isDanglingChild = false,
                        attachPrecompiledBinaries = attachPrecompiledBinaries
                    )
                    register(spec, file, alwaysAttachFile = true)
                } else {
                    val spec = ChunkSpec(
                        originalModule = module,
                        effectiveModule = substitute(module.contextModule),
                        kind = chunkKind,
                        isDanglingChild = true,
                        attachPrecompiledBinaries = attachPrecompiledBinaries
                    )
                    register(spec, file, alwaysAttachFile = true)
                }
            }

            module.isSupported && chunkKind != CompilationChunkKind.MAIN -> {
                val spec = ChunkSpec(
                    originalModule = module,
                    effectiveModule = module,
                    kind = chunkKind,
                    isDanglingChild = false,
                    attachPrecompiledBinaries = attachPrecompiledBinaries
                )
                register(spec, file)
            }

            else -> {
                val substitutedModule = substitute(module)
                // If the module is still not a JVM one, we have to create a dangling file module for it, as the JVM compiler
                // cannot compile files from common modules. More precisely, it can consume files with 'expect' declarations,
                // but dependencies of common modules are also common. From those, the backend cannot get the required information
                // (JVM facade class names, bytecode for inlining, and so on).
                val spec = ChunkSpec(
                    originalModule = module,
                    effectiveModule = substitutedModule,
                    kind = chunkKind,
                    isDanglingChild = module != substitutedModule || !module.targetPlatform.isJvm(),
                    attachPrecompiledBinaries = attachPrecompiledBinaries
                )
                register(spec, file)
            }
        }
    }

    private fun shouldAttachPrecompiledBinaries(file: KtFile): Boolean {
        if (file is KtCodeFragment) {
            val contextFile = file.context?.containingFile
            return if (contextFile is KtFile) shouldAttachPrecompiledBinaries(contextFile) else false
        }

        // Support for debugging in the Kotlin compiler repository.
        // There, the 'kotlin-stdlib' comes as a source, and compiling it is non-trivial and inefficient.
        return USE_STDLIB_BUILD_OUTPUT && file.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
    }

    /**
     * Whether the [KaFirCompilerFacility] supports compilation of the given module.
     * Currently, only JVM modules are supported.
     */
    private val KaModule.isSupported: Boolean
        get() = targetPlatform.isJvm()

    private val KaModule.implementingJvmModule: KaModule?
        get() = actualizer?.actualize(this)

    private val moduleCache = HashMap<KaModule, KaModule>()

    private fun substitute(module: KaModule): KaModule {
        require(module !is KaDanglingFileModule) { "Compilation of nested dangling file modules is not supported" }

        return moduleCache.computeIfAbsent(module) { module ->
            if (module.targetPlatform.isCommon()) {
                module.implementingJvmModule ?: module
            } else {
                module
            }
        }
    }

    /**
     * Compute chunks containing all files passed to [submit].
     *
     * The main chunk is guaranteed to be the last one in the returned [Map].
     * Other chunks generally follow the order of file submission.
     */
    fun computeChunks(): Map<KaModule, CompilationChunk> {
        val [mainChunks, otherChunks] = submittedChunks.entries.partition { it.key.kind == CompilationChunkKind.MAIN }
        val result = LinkedHashMap<KaModule, CompilationChunk>()

        // Contains mappings from original modules to the modules that should be used instead.
        // The mappings are used for substituting context modules of the dependent dangling file chunks.
        // E.g., if 'common' is substituted with 'jvm', a code fragment with the 'common' context module
        // becomes a new dangling file module with the 'jvm' context.
        val moduleSubstitutions = HashMap<KaModule, KaModule>()

        // Cross-module file substitutions.
        // Used for patching context of code fragments.
        val fileSubstitutions = HashMap<KtFile, KtFile>()

        tailrec fun mapModule(originalModule: KaModule): KaModule? {
            if (originalModule is KaDanglingFileModule) {
                return mapModule(originalModule.contextModule)
            }
            return moduleSubstitutions[originalModule]
        }

        /**
         * Create a new multi-file dangling file module, containing copies of [files], with the specified [contextModule].
         */
        fun appendDanglingChunk(spec: ChunkSpec, files: List<KtFile>) {
            val newFiles = buildList(files.size) {
                for (file in files) {
                    val fileCopy = createFileCopy(file, fileSubstitutions)
                    fileSubstitutions[file] = fileCopy
                    add(fileCopy)
                }
            }

            val mainFile = if (spec.kind == CompilationChunkKind.MAIN) {
                val mainFileIndex = files.indexOf(originalMainFile)
                check(mainFileIndex >= 0) { "Main file is not submitted" }
                newFiles[mainFileIndex]
            } else {
                null
            }

            val contextModule = mapModule(spec.originalModule) ?: spec.effectiveModule
            val newModule = createDanglingFileModule(newFiles, contextModule)
            newFiles.forEach { it.explicitModule = newModule }

            moduleSubstitutions[spec.originalModule] = newModule

            result[newModule] = CompilationChunk(
                spec.kind,
                mainFile = mainFile,
                hasCodeFragments = newFiles.any { it is KtCodeFragment },
                attachPrecompiledBinaries = spec.attachPrecompiledBinaries,
                files = createFilesToCompile(newFiles)
            )
        }

        fun process(entries: List<Map.Entry<ChunkSpec, Set<KtFile>>>) {
            for ([spec, files] in entries) {
                if (spec.isDanglingChild) {
                    // Creation of the new dangling file module is explicitly requested.
                    appendDanglingChunk(spec, files.toList())
                } else {
                    val hasCodeFragments = files.any { it is KtCodeFragment }

                    val mainFile = if (spec.kind == CompilationChunkKind.MAIN) {
                        check(originalMainFile in files) { "Main file is not submitted" }
                        originalMainFile
                    } else {
                        null
                    }

                    moduleSubstitutions[spec.originalModule] = spec.effectiveModule

                    result[spec.effectiveModule] = CompilationChunk(
                        kind = spec.kind,
                        mainFile = mainFile,
                        hasCodeFragments = hasCodeFragments,
                        attachPrecompiledBinaries = spec.attachPrecompiledBinaries,
                        files = createFilesToCompile(files)
                    )
                }
            }
        }

        // The main chunk needs to be the last one
        process(otherChunks)
        process(mainChunks)

        return result
    }

    fun createDanglingFileModule(newFiles: List<KtFile>, contextModule: KaModule): KaDanglingFileModule {
        val baseModule = KaDanglingFileModuleImpl(newFiles, contextModule, KaDanglingFileResolutionMode.PREFER_SELF)

        if (!contextModule.targetPlatform.isJvm()) {
            return createJvmDanglingFileModule(baseModule, contextModule)
        }

        return baseModule
    }

    /**
     * Create a custom dangling file module with the substituted target platform and dependencies.
     *
     * For multiplatform modules with common and implementation parts, we can use the JVM implementation module for compiling
     * common declarations. However, in cases when there is no JVM module at all, we have to create one.
     */
    private fun createJvmDanglingFileModule(baseModule: KaDanglingFileModule, contextModule: KaModule): KaDanglingFileModule {
        val dependentSourceModules = KotlinModuleDependentsProvider.getInstance(project)
            .getTransitiveDependents(contextModule)
            .asSequence()
            .filterIsInstance<KaSourceModule>()
            .filter { it.targetPlatform.isJvm() }
            .sortedBy { it.name }
            .toList()

        val minimalJvmTarget = dependentSourceModules
            .asSequence()
            .flatMap { it.targetPlatform.componentPlatforms }
            .distinct()
            .mapNotNull { it.targetPlatformVersion as? JvmTarget }
            .sortedBy { it.majorVersion }
            .firstOrNull()

        // Check in which JVM modules the context module is used and pick the lowest JVM target,
        // so it is compatible with all usages.
        val minimalTargetPlatform = minimalJvmTarget?.let(JvmPlatforms::jvmPlatformByTargetVersion)
            ?: JvmPlatforms.defaultJvmPlatform

        val newBinaryDependencies = mergeJvmImplementationBinaryDependencies(dependentSourceModules)
        val newSourceDependencies = collectJvmImplementationSourceDependencies(contextModule)

        val newDependencies = sequenceOf(newBinaryDependencies, newSourceDependencies)
            .flatten()
            .toList()

        return object : KaDanglingFileModule by baseModule {
            override val targetPlatform: TargetPlatform
                get() = minimalTargetPlatform

            override val directRegularDependencies: List<KaModule>
                get() = newDependencies
        }
    }

    /**
     * Collect JVM counterparts of source modules on which the [contextModule] depends.
     * These would be (at least some of the) dependencies of the JVM counterpart of the [contextModule] itself.
     */
    private fun collectJvmImplementationSourceDependencies(contextModule: KaModule): Sequence<KaSourceModule> {
        return sequence {
            val processedModules = HashSet<KaModule>()

            val pendingModules = ArrayDeque<KaModule>()
            pendingModules.addLast(contextModule)

            while (pendingModules.isNotEmpty()) {
                val module = pendingModules.removeLast()

                for (dependency in module.directRegularDependencies) {
                    if (dependency is KaSourceModule && processedModules.add(dependency)) {
                        val dependencyImplementing = dependency.implementingJvmModule as? KaSourceModule
                        val dependencyEffective = dependencyImplementing ?: dependency

                        // Check for duplicates once more – we could get the implementing module through JVM dependencies
                        // and also through the common module implementation
                        if (dependencyImplementing == null || processedModules.add(dependencyEffective)) {
                            yield(dependencyEffective)

                            // Also, add source dependencies of the implementing module.
                            // Note that this adds dependencies to modules unavailable in the original context.
                            // This may lead to a classpath hell – however, not adding the dependencies can also lead to
                            // failure in code inlining.
                            pendingModules.addLast(dependencyEffective)
                        }
                    }
                }
            }
        }
    }

    /**
     * Collect common JVM binary module dependencies between all provided [dependentSourceModules].
     * The goal is to figure out what dependencies should be provided instead of the dependency to a common module.
     */
    private fun mergeJvmImplementationBinaryDependencies(dependentSourceModules: List<KaSourceModule>): Sequence<KaLibraryModule> {
        return sequence {
            val dependentModuleDependencies = dependentSourceModules
                .map { collectJvmImplementationBinaryDependencies(it) }

            var sdkModule: KaLibraryModule? = null

            val commonBinaryDependencies = LinkedHashSet<KaLibraryModule>()
            var isFirst = true

            for (dependencySequence in dependentModuleDependencies) {
                val dependencies = dependencySequence.toList()

                if (sdkModule == null) {
                    // Pick the first SDK for now.
                    // In the scope of KT-77426, this should become customizable.
                    sdkModule = dependencies.firstOrNull { it.isSdk }
                }

                if (isFirst) {
                    commonBinaryDependencies.addAll(dependencies)
                    isFirst = false
                } else {
                    commonBinaryDependencies.retainAll(dependencies)
                }
            }

            if (sdkModule != null) {
                commonBinaryDependencies.remove(sdkModule)
            }

            yieldIfNotNull(sdkModule)
            yieldAll(commonBinaryDependencies)
        }
    }

    /**
     * Collect JVM binary module dependencies of the given [contextModule], including transitive dependencies.
     */
    private fun collectJvmImplementationBinaryDependencies(contextModule: KaModule): Sequence<KaLibraryModule> {
        return sequence {
            val processedModules = HashSet<KaModule>()

            val pendingModules = ArrayDeque<KaModule>()
            pendingModules.addLast(contextModule)

            while (pendingModules.isNotEmpty()) {
                val module = pendingModules.removeLast()
                for (dependency in module.directRegularDependencies) {
                    if (!processedModules.add(dependency)) {
                        continue
                    }

                    if (dependency is KaLibraryModule) {
                        if (dependency.targetPlatform.isJvm()) {
                            yield(dependency)
                        }
                    } else {
                        pendingModules.addLast(dependency)
                    }
                }
            }
        }
    }

    private fun createFilesToCompile(files: Collection<KtFile>): List<CompilationChunkFile> {
        return buildList {
            for (file in files) {
                val firFile = if (file == originalMainFile) originalMainFirFile else getFullyResolvedFirFile(file, resolutionFacade)
                add(CompilationChunkFile(file, firFile))
            }

            // Code fragments must go after all context files so the backend works correctly
            sortBy { it.ktFile is KtCodeFragment }
        }
    }

    /**
     * Creates a copy of the [file].
     *
     * For code fragments, the context is substituted using [fileSubstitutions].
     * File content in substitution key-value pairs must be identical.
     */
    private fun createFileCopy(file: KtFile, fileSubstitutions: Map<KtFile, KtFile>): KtFile {
        val newName = file.name
        val newText = file.text

        if (file is KtCodeFragment) {
            val newContext = substituteContext(file.context, fileSubstitutions)

            return when (file) {
                is KtExpressionCodeFragment -> KtExpressionCodeFragment(project, newName, newText, file.importsToString(), newContext)
                is KtBlockCodeFragment -> KtBlockCodeFragment(project, newName, newText, file.importsToString(), newContext)
                is KtTypeCodeFragment -> KtTypeCodeFragment(project, newName, newText, newContext)
                else -> error("Unsupported code fragment type: " + file.javaClass.name)
            }
        }

        return KtPsiFactory(project).createPhysicalFile(newName, newText)
    }

    private fun substituteContext(context: PsiElement?, fileSubstitutions: Map<KtFile, KtFile>): PsiElement? {
        val containingFile = context?.containingFile ?: return context
        val substitutedFile = fileSubstitutions[containingFile] ?: return context
        return PsiTreeUtil.findSameElementInCopy(context, substitutedFile)
    }
}
