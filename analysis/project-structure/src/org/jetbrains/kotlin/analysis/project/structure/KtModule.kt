/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import java.nio.file.Path

/**
 * Represents a module inside a project.
 *
 * [KtModule] is a Source Set (or considering a new project model naming a Fragment).
 * Some examples of a module: main source set, test source set, library, JDK.
 *
 */
public sealed interface KtModule {
    /**
     * A list of Regular dependencies. Regular dependency allows the current module to see symbols from the dependent module. In the case
     * of a source set, it can be either the source set it depends on, a library, or an SDK.
     *
     * The dependencies list is non-transitive and does not include the current module.
     */
    public val directRegularDependencies: List<KtModule>

    /**
     * A list of `dependsOn` dependencies. (Kotlin MPP projects only.)
     *
     * A `dependsOn` dependency expresses that the current module can provide `actual` declarations for `expect` declarations from the
     * dependent module, as well as see internal symbols of the dependent module.
     *
     * `dependsOn` dependencies are transitive, but the list is not a transitive closure. The list does not include the current module.
     */
    public val directDependsOnDependencies: List<KtModule>

    /**
     * A list of [directDependsOnDependencies] and all of their parents (directly and indirectly), sorted topologically with the nearest
     * dependencies first in the list. The list does not include the current module.
     *
     * @see computeTransitiveDependsOnDependencies
     */
    public val transitiveDependsOnDependencies: List<KtModule>

    /**
     * A list of Friend dependencies. Friend dependencies express that the current module may see internal symbols of the dependent module.
     *
     * The dependencies list is non-transitive and does not include the current module.
     */
    public val directFriendDependencies: List<KtModule>

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
    public val platform: TargetPlatform

    public val analyzerServices: PlatformDependentAnalyzerServices

    /**
     * [Project] to which the current module belongs.
     *
     * If the current module depends on some other modules, all those modules should have the same [Project] as the current one.
     */
    public val project: Project

    /**
     * A human-readable description of the current module. E.g, "main sources of module 'analysis-api'".
     */
    public val moduleDescription: String
}

/**
 * A module which consists of a set of source declarations inside a project.
 *
 * Generally, a main or test Source Set.
 */
public interface KtSourceModule : KtModule {
    public val moduleName: String

    /**
     * A stable binary name of module from the *Kotlin* point of view.
     * Having correct module name is critical for `internal`-visibility mangling. See [org.jetbrains.kotlin.asJava.mangleInternalName]
     */
    public val stableModuleName: String?
        get() = null

    override val moduleDescription: String
        get() = "Sources of $moduleName"

    /**
     * A set of Kotlin settings, like API version, supported features and flags.
     */
    public val languageVersionSettings: LanguageVersionSettings
}

/**
 * A module which consists of binary declarations.
 */
public sealed interface KtBinaryModule : KtModule {
    /**
     * A list of binary files which forms a binary module. It can be a list of JARs, KLIBs, folders with .class files.
     *
     * It should be consistent with [contentScope], so (pseudo-Kotlin):
     * ```
     * library.contentScope.contains(file) <=> library.getBinaryRoots().listRecursively().contains(file)
     * ```
     */
    public fun getBinaryRoots(): Collection<Path>
}

/**
 * A module which represents a binary library, e.g. JAR or KLIB.
 */
public interface KtLibraryModule : KtBinaryModule {
    public val libraryName: String

    /**
     * A library source, if any. If current module is a binary JAR, then [librarySources] corresponds to the sources JAR.
     */
    public val librarySources: KtLibrarySourceModule?

    override val moduleDescription: String
        get() = "Library $libraryName"
}

/**
 * A module which represent some SDK, e.g. Java JDK.
 */
public interface KtSdkModule : KtBinaryModule {
    public val sdkName: String

    override val moduleDescription: String
        get() = "SDK $sdkName"
}

/**
 * Sources for some [KtLibraryModule].
 */
public interface KtLibrarySourceModule : KtModule {
    public val libraryName: String

    /**
     * A library binary corresponding to the current library source.
     * If the current module is a source JAR, then [binaryLibrary] corresponds to the binaries JAR.
     */
    public val binaryLibrary: KtLibraryModule

    override val moduleDescription: String
        get() = "Library sources of $libraryName"
}

/**
 * A module which contains kotlin [builtins](https://kotlinlang.org/spec/built-in-types-and-their-semantics.html) for a specific platform.
 * Kotlin builtins usually reside in the compiler, so [contentScope] and [getBinaryRoots] are empty.
 */
public class KtBuiltinsModule(
    override val platform: TargetPlatform,
    override val analyzerServices: PlatformDependentAnalyzerServices,
    override val project: Project
) : KtBinaryModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directDependsOnDependencies: List<KtModule> get() = emptyList()
    override val transitiveDependsOnDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()
    override val contentScope: GlobalSearchScope get() = GlobalSearchScope.EMPTY_SCOPE
    override fun getBinaryRoots(): Collection<Path> = emptyList()
    override val moduleDescription: String get() = "Builtins for $platform"

    override fun equals(other: Any?): Boolean = other is KtBuiltinsModule && this.platform == other.platform
    override fun hashCode(): Int = platform.hashCode()
}

/**
 * A module for a Kotlin script file.
 */
public interface KtScriptModule : KtModule {
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
 * Must be either a [KtLibraryModule] or [KtLibrarySourceModule].
 */
public interface KtScriptDependencyModule : KtModule {
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
public interface KtDanglingFileModule : KtModule {
    /**
     * A temporary file PSI.
     */
    public val file: KtFile

    /**
     * The module against which the [file] is analyzed.
     */
    public val contextModule: KtModule

    /**
     * A way of resolving references to non-local declarations in the dangling file.
     */
    public val resolutionMode: DanglingFileResolutionMode

    /**
     * True if the [file] is a code fragment.
     * Useful to recognize code fragments when their PSI was collected.
     */
    public val isCodeFragment: Boolean

    override val moduleDescription: String
        get() = "Temporary file"
}

/**
 * True if the dangling file module supports partial invalidation on PSI modifications.
 * Sessions for such modules can be cached for longer time.
 */
public val KtDanglingFileModule.isStable: Boolean
    get() = file.isPhysical && file.viewProvider.isEventSystemEnabled

/**
 * A set of sources which live outside the project content root. E.g, testdata files or source files of some other project.
 */
public interface KtNotUnderContentRootModule : KtModule {
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
