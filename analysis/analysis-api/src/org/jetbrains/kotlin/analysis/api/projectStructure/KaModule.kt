/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
     * A base content scope of the module, which is not yet refined by extension and restriction scopes provided by
     * `KotlinContentScopeRefiner`.
     *
     * Note that [baseContentScope] doesn't represent the actual content scope of the current module. To get a content scope of the module,
     * [contentScope] should be used instead.
     */
    @KaPlatformInterface
    public val baseContentScope: GlobalSearchScope

    /**
     * Represents the content scope of a current module, i.e., a [GlobalSearchScope] which determines all the files that are contained in
     * the module.
     *
     * This scope should be lazily built from [baseContentScope] using `KotlinContentScopeRefiner` extension points.
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
 * Whether the given [KaModule] can be the use-site module of an [analyze][org.jetbrains.kotlin.analysis.api.analyze] call. A module which
 * is not resolvable will be rejected by [analyze][org.jetbrains.kotlin.analysis.api.analyze] with an exception.
 *
 * All modules returned by [KaModuleProvider.getModule] are guaranteed to be resolvable. By extension, all possible use-site [PsiElement][com.intellij.psi.PsiElement]s
 * are also part of resolvable modules. As such, module resolvability is normally not a concern of an Analysis API user.
 */
@KaPlatformInterface
public val KaModule.isResolvable: Boolean
    get() = this !is KaLibraryFallbackDependenciesModule

/**
 * A [KaModule] representing a set of source declarations.
 *
 * A [KaSourceModule] does not necessarily have to correspond directly to an Analysis API platform's concept of a "module." For example, the
 * IntelliJ implementation distinguishes between production and test source sets. As such, the `src` and `test` source sets of an IntelliJ
 * module are actually different [KaSourceModule]s. To allow a test source module to use the internal declarations from the production
 * source module, the test source module defines a [friend dependency][directFriendDependencies] on the production source module.
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
 *
 * ### Dependencies
 *
 * [KaLibraryModule]s can have their own dependencies (e.g. [directRegularDependencies]). These dependencies are only relevant when the
 * library is analyzed as a use-site module (e.g. its decompiled sources are viewed in the IDE). When the library module is used as a
 * dependency of another module, its own dependencies are irrelevant.
 *
 * The library module should either have the exact dependencies it was compiled with or, if unknown, a single
 * [KaLibraryFallbackDependenciesModule].
 *
 * ### Platform-specific content scope restriction
 *
 * In the K2 implementation of the Analysis API, the [contentScope] of the library module is restricted to file types that are relevant for
 * the [targetPlatform]. For example, a JVM library module filters out any files that are not `.class` and `.kotlin_builtins` files. This
 * allows the Analysis API to exclude content which isn't relevant for the target platform, such as `.knm` files in a JVM library.
 *
 * While most proper library module setups don't need such filtering, there are both pathological as well as legitimate use cases in the
 * wild. For example, certain Kotlin stdlib setups required both the `kotlin-stdlib` and `kotlin-stdlib-common` JARs to be part of the same
 * [KaLibraryModule] (this has been fixed with 2.x stdlibs). Such a library module has the JVM target platform, and we need to exclude
 * `.kotlin_metadata` files from the content scope.
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
            @OptIn(KaPlatformInterface::class)
            val label = if (isSdk) "SDK" else "Library"
            return "$label $libraryName"
        }
}

/**
 * A module which represents the sources for a [KaLibraryModule].
 *
 * For example, when viewing a library file in an IDE, the library sources are usually preferred over the library's binary files (if
 * available). The [KaLibrarySourceModule] represents exactly such sources.
 *
 * The library source module's dependencies must be the same as its [binaryLibrary]'s dependencies. In particular, the library source module
 * must also depend on a single [KaLibraryFallbackDependenciesModule] if its exact dependencies are unknown.
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
 * A module which stands in for the *unknown* dependencies of a [KaLibraryModule] and [KaLibrarySourceModule].
 *
 * Files in library (source) modules can be resolved with the Analysis API. From such a resolvable point of view, the Analysis API needs to
 * find symbols which are defined in the library's dependencies. However, the dependencies with which a library was originally compiled are
 * often not known.
 *
 * As a replacement for precise dependencies, *fallback dependencies* cover all libraries in the project except for the specific
 * [dependentLibrary]. This allows resolving symbols defined in the dependencies of the library. In most cases, while not perfectly precise,
 * this approach resolves the correct symbols.
 *
 * The fallback dependencies module's [baseContentScope] should be the scope of all libraries excluding [dependentLibrary]. It should have
 * the same [targetPlatform] as [dependentLibrary].
 *
 * [KaLibraryFallbackDependenciesModule] is not [resolvable][isResolvable] and thus cannot be a use-site module of an [analyze][org.jetbrains.kotlin.analysis.api.analyze]
 * call. It should not be returned by [KaModuleProvider.getModule].
 *
 * The content of the fallback dependencies module is filtered by the target platform in exactly the same way as [KaLibraryModule]s. Please
 * see the KDoc of [KaLibraryModule] for more information.
 */
@KaPlatformInterface
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaLibraryFallbackDependenciesModule : KaModule {
    /**
     * The [KaLibraryModule] which relies on these fallback dependencies.
     *
     * Both the [dependentLibrary] and its [KaLibrarySourceModule] may depend on this same fallback dependencies module. There is no
     * separate fallback dependencies module for the library source module.
     */
    public val dependentLibrary: KaLibraryModule
}

/**
 * A module which contains Kotlin [builtins](https://kotlinlang.org/spec/built-in-types-and-their-semantics.html) for a specific platform.
 *
 * [KaBuiltinsModule] is a *fallback module* which, as a dependency, provides builtins for modules that don't have an associated Kotlin
 * stdlib. Usually, a stdlib [KaLibraryModule] will have a higher precedence in dependencies and builtins will be resolved from there.
 *
 * Modules normally don't depend explicitly on [KaBuiltinsModule]. Rather, this dependency is materialized internally by the Analysis API's
 * resolution engine.
 */
@KaPlatformInterface
@SubclassOptInRequired(KaPlatformInterface::class)
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
 *
 * Script dependencies are self-contained and should not depend on other libraries, not even [KaLibraryFallbackDependenciesModule].
 */
@KaPlatformInterface
@SubclassOptInRequired(KaPlatformInterface::class)
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
@SubclassOptInRequired(KaPlatformInterface::class)
public interface KaDanglingFileModule : KaModule {
    /**
     * The dangling file.
     */
    @Deprecated(
        "Use 'files' instead.",
        ReplaceWith("files.single()", imports = ["kotlin.collections.single"])
    )
    public val file: KtFile
        get() = files.first()

    /**
     * All dangling files analyzed together, as a single module.
     *
     * Throws an exception when the files are no longer valid (see [isValid]).
     */
    public val files: List<KtFile>

    /**
     * The module against which [files] are analyzed.
     */
    public val contextModule: KaModule

    /**
     * The mode which determines how references to non-local declarations in the dangling file are resolved.
     */
    public val resolutionMode: KaDanglingFileResolutionMode

    /**
     * Whether at least one of the [files] is a code fragment.
     *
     * This is useful to recognize code fragments when their PSI was collected.
     */
    public val isCodeFragment: Boolean

    /**
     * Whether the dangling file module's [files] are still valid.
     *
     * @see KtFile.isValid
     */
    public val isValid: Boolean

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Temporary file"
}

/**
 * Whether the dangling file module supports partial invalidation on PSI modifications. The sessions for such modules can be cached for a
 * longer time.
 */
@OptIn(KaPlatformInterface::class)
public val KaDanglingFileModule.isStable: Boolean
    get() = files.all { it.isPhysical && it.viewProvider.isEventSystemEnabled }

/**
 * A module which represents a source file living outside the project's content root. For example, test data files, or the source files of
 * another project.
 *
 * Depending on the Analysis API platform implementation, the [KaNotUnderContentRootModule] may have dependencies, e.g., dependencies on the Kotlin standard library or the JDK.
 */
@KaPlatformInterface
@SubclassOptInRequired(KaPlatformInterface::class)
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
