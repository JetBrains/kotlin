/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaPlatformInterface::class)

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * [KaModule] is the Analysis API's view on a module inside a project. A [KaModule] represents a [source set][KaSourceModule] (with
 * production and test sources being separate modules), [script][KaScriptModule], [library][KaLibraryModule], and various niche concepts
 * such as [dangling files][KaDanglingFileModule].
 *
 * As an Analysis API user, you will mainly interact with [KaModule]s indirectly when providing a use-site module to an
 * [analyze][org.jetbrains.kotlin.analysis.api.analyze] call (usually derived from the use-site [KtElement][org.jetbrains.kotlin.psi.KtElement]
 * instead). However, it is possible to utilize the Analysis API's module support to explore the project structure and retrieve information
 * about individual modules when needed.
 *
 * Modules are crucial to establish the use-site view of [analysis sessions][org.jetbrains.kotlin.analysis.api.KaSession]. An analysis
 * session always exists in the context of a particular use-site module. The module determines the session's [resolution scope][org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider.analysisScope],
 * and generally the content and dependencies from which [symbols][org.jetbrains.kotlin.analysis.api.symbols.KaSymbol] are resolved.
 *
 * Individual [KaModule]s for specific [PsiElements][com.intellij.psi.PsiElement] can be retrieved from [KaModuleProvider]. The
 * implementation of this provider, and the project structure in general, are provided by the [Analysis API platform](https://github.com/JetBrains/kotlin/tree/master/analysis/analysis-api-platform-interface),
 * such as IntelliJ or Standalone. As such, [KaModule] implementations are not provided by the Analysis API engine (in contrast to most of
 * the Analysis API surface), but rather by individual platforms.
 *
 * Modules are also the Analysis API's unit of modification. When a code change has possible non-local consequences (i.e. it happens outside
 * an isolated function or property body), the resulting cache invalidation occurs on the level of a module and its dependents. While
 * modification handling overall is an Analysis API *platform* topic, it is important to establish this basic fact.
 */
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaModule {
    /**
     * The module's regular dependencies. Regular dependencies allow the current module to use symbols from the dependency module.
     *
     * The resulting list is not transitive and does not include the current module.
     */
    public val directRegularDependencies: List<KaModule>

    /**
     * The module's [`dependsOn` dependencies](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/org.jetbrains.kotlin.gradle.plugin/-kotlin-source-set/depends-on.html).
     *
     * A `dependsOn` dependency expresses that the current module can provide `actual` declarations for `expect` declarations from the
     * dependency module, as well as see internal symbols of the dependency module. As such, `dependsOn` dependencies are a Kotlin
     * Multiplatform concept.
     *
     * `dependsOn` dependencies in general are transitive, but the resulting list is not a transitive closure. To get all transitive
     * `dependsOn` dependencies, [transitiveDependsOnDependencies] should be used. The list also does not include the current module.
     */
    public val directDependsOnDependencies: List<KaModule>

    /**
     * A list of [directDependsOnDependencies] and all of their own `dependsOn` dependencies (directly and indirectly), sorted topologically
     * with the nearest dependencies first in the list. The list does not include the current module.
     */
    public val transitiveDependsOnDependencies: List<KaModule>

    /**
     * The module's friend dependencies. Friend dependencies allow the current module to use internal symbols from the dependency module.
     *
     * The resulting list is not transitive and does not include the current module.
     */
    public val directFriendDependencies: List<KaModule>

    /**
     * A [GlobalSearchScope] which determines all the files that are contained in the module.
     */
    public val contentScope: GlobalSearchScope

    /**
     * A platform which the module represents (e.g, JVM, JS, Native).
     *
     * @see [TargetPlatform]
     */
    public val targetPlatform: TargetPlatform

    /**
     * The [Project] to which the module belongs.
     *
     * All the module's dependencies should belong to the same [Project] as the module itself.
     */
    public val project: Project

    /**
     * A human-readable description of the module, such as "main sources of module 'analysis-api'".
     */
    @KaExperimentalApi
    public val moduleDescription: String

    /**
     * A stable binary name of module from the *Kotlin* point of view. Having a correct module name is critical for `internal`-visibility
     * mangling.
     *
     * NOTE: [stableModuleName] will be removed in the future and replaced with a platform interface service.
     */
    @KaExperimentalApi
    public val stableModuleName: String?
        get() = null
}

/**
 * A [KaModule] representing a set of source declarations.
 *
 * The Analysis API distinguishes between production and test source sets. As such, the `src` and `test` source sets of a "module" are
 * actually different [KaSourceModule]s. To allow a test source module to use the declarations from the production source module, the test
 * source module generally defines a [friend dependency][directFriendDependencies] on the production source module.
 */
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaSourceModule : KaModule {
    /**
     * The name of the module.
     *
     * In practice, the specific format of the [name] depends on the Analysis API platform which provides the [KaSourceModule]
     * implementation.
     */
    public val name: String

    /**
     * A set of Kotlin language settings, such as the API version, supported features, and flags.
     */
    public val languageVersionSettings: LanguageVersionSettings

    /**
     * The PSI-specific view on the source roots of the module.
     */
    @KaExperimentalApi
    public val psiRoots: List<PsiFileSystemItem>
        get() = listOf()

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Sources of $name"
}

/**
 * A module which represents a binary library, such as a JAR or KLIB.
 */
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaLibraryModule : KaModule {
    /**
     * The name of the library.
     *
     * In practice, the specific format of the [libraryName] depends on the Analysis API platform which provides the [KaLibraryModule]
     * implementation.
     */
    public val libraryName: String

    /**
     * A list of binary roots which constitute the library. The list can contain JARs, KLIBs, folders with `.class` files, and so on.
     *
     * The paths should be consistent with the [contentScope], so the following equivalence should hold:
     *
     * ```
     * library.contentScope.contains(file) <=> library.binaryRoots.listRecursively().contains(file)
     * ```
     */
    public val binaryRoots: Collection<Path>

    /**
     * A list of binary files in [VirtualFile] form if the library module represents a library in an in-memory file system.
     */
    @KaExperimentalApi
    public val binaryVirtualFiles: Collection<VirtualFile>

    /**
     * The library sources for the binary library, if any.
     *
     * For example, if this module is a binary JAR, then [librarySources] corresponds to the sources JAR.
     */
    public val librarySources: KaLibrarySourceModule?

    /**
     * Whether the module represents an SDK, such as the JDK.
     */
    @KaPlatformInterface
    public val isSdk: Boolean

    @KaExperimentalApi
    override val moduleDescription: String
        get() {
            val label = if (isSdk) "SDK" else "Library"
            return "$label $libraryName"
        }
}

/**
 * A module which represents the sources for a [KaLibraryModule].
 *
 * For example, when viewing a library file in an IDE, the library sources are usually preferred over the library's binary files (if
 * available). The [KaLibrarySourceModule] represents exactly such sources.
 */
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaLibrarySourceModule : KaModule {
    /**
     * The name of the library sources.
     *
     * In practice, the specific format of the [libraryName] depends on the Analysis API platform which provides the [KaLibraryModule]
     * implementation.
     */
    public val libraryName: String

    /**
     * The [binary library][KaLibraryModule] which corresponds to the library sources.
     *
     * For example, if this module is a source JAR, then [binaryLibrary] corresponds to the binary JAR.
     */
    public val binaryLibrary: KaLibraryModule

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Library sources of $libraryName"
}

/**
 * A module which contains Kotlin [builtins](https://kotlinlang.org/spec/built-in-types-and-their-semantics.html) for a specific platform.
 */
@KaPlatformInterface
public interface KaBuiltinsModule : KaModule {
    override val directRegularDependencies: List<KaModule> get() = emptyList()
    override val directDependsOnDependencies: List<KaModule> get() = emptyList()
    override val transitiveDependsOnDependencies: List<KaModule> get() = emptyList()
    override val directFriendDependencies: List<KaModule> get() = emptyList()

    @KaExperimentalApi
    override val moduleDescription: String get() = "Builtins for $targetPlatform"
}

/**
 * A module for a Kotlin script file.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaScriptModule : KaModule {
    /**
     * The [KtFile] which contains the Kotlin script.
     */
    public val file: KtFile

    /**
     * A set of Kotlin language settings, such as the API version, supported features, and flags.
     */
    public val languageVersionSettings: LanguageVersionSettings

    override val moduleDescription: String
        get() = "Script " + file.name
}

/**
 * A module for Kotlin script dependencies. Must either be a [KaLibraryModule] or [KaLibrarySourceModule].
 */
@KaPlatformInterface
public interface KaScriptDependencyModule : KaModule {
    /**
     * The [KtFile] that backs the PSI of the script dependency, or `null` if the module is for project-level dependencies.
     */
    public val file: KtFile?
}

/**
 * A module for a dangling file. Such files are usually temporary and are stored in-memory.
 *
 * Dangling files may be created for various purposes, such as: a code fragment for the evaluator, a sandbox for testing code modification
 * applicability, and so on.
 */
@KaPlatformInterface
public interface KaDanglingFileModule : KaModule {
    /**
     * The dangling file.
     */
    public val file: KtFile

    /**
     * The module against which the [file] is analyzed.
     */
    public val contextModule: KaModule

    /**
     * The mode which determines how references to non-local declarations in the dangling file are resolved.
     */
    public val resolutionMode: KaDanglingFileResolutionMode

    /**
     * Whether the [file] is a code fragment.
     *
     * This is useful to recognize code fragments when their PSI was collected.
     */
    public val isCodeFragment: Boolean

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Temporary file"
}

/**
 * Whether the dangling file module supports partial invalidation on PSI modifications. The sessions for such modules can be cached for a
 * longer time.
 */
public val KaDanglingFileModule.isStable: Boolean
    get() = file.isPhysical && file.viewProvider.isEventSystemEnabled

/**
 * A module which represents a source file living outside the project's content root. For example, test data files, or the source files of
 * another project.
 */
@KaPlatformInterface
public interface KaNotUnderContentRootModule : KaModule {
    /**
     * A human-readable module name.
     */
    public val name: String

    /**
     * The [PsiFile] which this module represents. A separate module is created for each file outside a content root.
     */
    public val file: PsiFile?
        get() = null
}
