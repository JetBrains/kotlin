/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaPlatformInterface::class)

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * Represents a module inside a project.
 *
 * [KaModule] is a Source Set (or considering a new project model naming a Fragment).
 * Some examples of a module: main source set, test source set, library, JDK.
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(markerClass = KaPlatformInterface::class)
public interface KaModule {
    /**
     * A list of Regular dependencies. Regular dependency allows the current module to see symbols from the dependent module. In the case
     * of a source set, it can be either the source set it depends on, a library, or an SDK.
     *
     * The dependencies list is non-transitive and does not include the current module.
     */
    public val directRegularDependencies: List<KaModule>

    /**
     * A list of `dependsOn` dependencies. (Kotlin MPP projects only.)
     *
     * A `dependsOn` dependency expresses that the current module can provide `actual` declarations for `expect` declarations from the
     * dependent module, as well as see internal symbols of the dependent module.
     *
     * `dependsOn` dependencies are transitive, but the list is not a transitive closure. The list does not include the current module.
     */
    public val directDependsOnDependencies: List<KaModule>

    /**
     * A list of [directDependsOnDependencies] and all of their parents (directly and indirectly), sorted topologically with the nearest
     * dependencies first in the list. The list does not include the current module.
     */
    public val transitiveDependsOnDependencies: List<KaModule>

    /**
     * A list of Friend dependencies. Friend dependencies express that the current module may see internal symbols of the dependent module.
     *
     * The dependencies list is non-transitive and does not include the current module.
     */
    public val directFriendDependencies: List<KaModule>

    /**
     * A [GlobalSearchScope] which belongs to a module content.
     *
     * Contract: `module.contentScope.contains(file) <=> file belongs to this module`
     */
    public val contentScope: GlobalSearchScope

    /**
     * A platform (e.g, JVM, JS, Native) which the current module represents.
     *
     * @see [TargetPlatform]
     */
    public val targetPlatform: TargetPlatform

    @Deprecated("Use 'targetPlatform' instead.", replaceWith = ReplaceWith("targetPlatform"))
    public val platform: TargetPlatform get() = targetPlatform

    /**
     * [Project] to which the current module belongs.
     *
     * If the current module depends on some other modules, all those modules should have the same [Project] as the current one.
     */
    public val project: Project

    /**
     * A human-readable description of the current module. E.g, "main sources of module 'analysis-api'".
     */
    @KaExperimentalApi
    public val moduleDescription: String
}

/**
 * A module which consists of a set of source declarations inside a project.
 *
 * Generally, a main or test Source Set.
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(markerClass = KaPlatformInterface::class)
public interface KaSourceModule : KaModule {
    public val name: String

    @Deprecated("Use 'name' instead.", replaceWith = ReplaceWith("name"))
    public val moduleName: String get() = name

    /**
     * A stable binary name of module from the *Kotlin* point of view.
     * Having correct module name is critical for `internal`-visibility mangling. See [org.jetbrains.kotlin.asJava.mangleInternalName]
     *
     * NOTE: [stableModuleName] will be removed in the future and replaced with a platform interface service.
     */
    @KaExperimentalApi
    public val stableModuleName: String?
        get() = null

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Sources of $name"

    /**
     * A set of Kotlin settings, like API version, supported features and flags.
     */
    public val languageVersionSettings: LanguageVersionSettings
}

/**
 * A module which represents a binary library, e.g. JAR or KLIB.
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(markerClass = KaPlatformInterface::class)
public interface KaLibraryModule : KaModule {
    public val libraryName: String

    /**
     * A list of binary files which constitute the library. The list can contain JARs, KLIBs, folders with `.class` files, and so on.
     *
     * The paths should be consistent with the [contentScope], so the following equivalence should hold:
     *
     * ```
     * library.contentScope.contains(file) <=> library.binaryRoots.listRecursively().contains(file)
     * ```
     */
    public val binaryRoots: Collection<Path>

    /**
     * A list of binary files in [VirtualFile] form if the library module represents a library in in-memory file system.
     */
    @KaExperimentalApi
    public val binaryVirtualFiles: Collection<VirtualFile>

    /**
     * A library source, if any. If current module is a binary JAR, then [librarySources] corresponds to the sources JAR.
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
 * Sources for some [KaLibraryModule].
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(markerClass = KaPlatformInterface::class)
public interface KaLibrarySourceModule : KaModule {
    public val libraryName: String

    /**
     * A library binary corresponding to the current library source.
     * If the current module is a source JAR, then [binaryLibrary] corresponds to the binaries JAR.
     */
    public val binaryLibrary: KaLibraryModule

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Library sources of $libraryName"
}

/**
 * A module which contains kotlin [builtins](https://kotlinlang.org/spec/built-in-types-and-their-semantics.html) for a specific platform.
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
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(markerClass = KaPlatformInterface::class)
public interface KaScriptModule : KaModule {
    /**
     * A script PSI.
     */
    public val file: KtFile

    /**
     * A set of Kotlin settings, like API version, supported features and flags.
     */
    public val languageVersionSettings: LanguageVersionSettings

    override val moduleDescription: String
        get() = "Script " + file.name
}

/**
 * A module for Kotlin script dependencies.
 * Must be either a [KaLibraryModule] or [KaLibrarySourceModule].
 */
@KaPlatformInterface
public interface KaScriptDependencyModule : KaModule {
    /**
     * A `VirtualFile` that backs the dependent script PSI, or `null` if the module is for project-level dependencies.
     */
    public val file: KtFile?
}

/**
 * A module for a dangling file. Such files are usually temporary and are stored in-memory.
 * Dangling files may be created for various purposes, such as: a code fragment for the evaluator, a sandbox for testing code modification
 * applicability, etc.
 */
@KaPlatformInterface
public interface KaDanglingFileModule : KaModule {
    /**
     * A temporary file PSI.
     */
    public val file: KtFile

    /**
     * The module against which the [file] is analyzed.
     */
    public val contextModule: KaModule

    /**
     * A way of resolving references to non-local declarations in the dangling file.
     */
    public val resolutionMode: KaDanglingFileResolutionMode

    /**
     * True if the [file] is a code fragment.
     * Useful to recognize code fragments when their PSI was collected.
     */
    public val isCodeFragment: Boolean

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Temporary file"
}

/**
 * True if the dangling file module supports partial invalidation on PSI modifications.
 * Sessions for such modules can be cached for longer time.
 */
public val KaDanglingFileModule.isStable: Boolean
    get() = file.isPhysical && file.viewProvider.isEventSystemEnabled

/**
 * A set of sources which live outside the project content root. E.g, testdata files or source files of some other project.
 */
@KaPlatformInterface
public interface KaNotUnderContentRootModule : KaModule {
    /**
     * Human-readable module name.
     */
    public val name: String

    /**
     * Module owner file.
     * A separate module is created for each file outside a content root.
     */
    public val file: PsiFile?
        get() = null
}
