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
     * A list of Regular dependencies. Regular dependency allows current module to see symbols from dependent module.
     * In a case for a source set, it can be other source set it depends on, library, or SDK.
     *
     * The dependencies list is non-transitive and should not include current module.
     */
    public val directRegularDependencies: List<KtModule>

    /**
     * Only for Kotlin MPP project.
     * A list of Refinement dependencies.
     * Refinement dependency express that the current module can provide actual declarations for expect declarations from dependent module,
     * as well as see internal symbols of dependent module.
     *
     * The dependencies list is non-transitive and should not include current module.
     */
    public val directRefinementDependencies: List<KtModule>

    /**
     * A list of Friend dependencies. Friend dependencies express that current module may see internal symbols of dependent module.
     *
     * The dependencies list is non-transitive and should not include current module.
     */
    public val directFriendDependencies: List<KtModule>

    /**
     * A [GlobalSearchScope] which belongs to a module content.
     *
     * Contract: `module.contentScope.contains(file) <=> file belongs to this module`
     */
    public val contentScope: GlobalSearchScope

    /**
     * A platform (e.g, JVM, JS, Native) current module represents.
     *
     * @see [TargetPlatform]
     */
    public val platform: TargetPlatform

    public val analyzerServices: PlatformDependentAnalyzerServices

    /**
     * [Project] to which module belongs.
     * If current module depends on some other modules, all those modules should have the same [Project] as the current one.
     */
    public val project: Project

    /**
     * Human-readable description of the module. E.g, "main sources of module 'analysis-api'"
     */
    public val moduleDescription: String
}

/**
 * A module which consists of a set of source declarations inside a projects.
 *
 * Generally, a main or test Source Set.
 */
public interface KtSourceModule : KtModule {
    public val moduleName: String

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
     * Should be consistent with [contentScope],
     * so (pseudo-Kotlin) `
     * ```
     * library.contentScope.contains(file) <=> library.getBinaryRoots().listRecursively().contains(file)
     * ```
     */
    public fun getBinaryRoots(): Collection<Path>
}

/**
 * A module which represents a binary library. E.g, JAR or KLIB.
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
 * A module which represent some SDK. E.g, Java JDK.
 */
public interface KtSdkModule : KtBinaryModule {
    public val sdkName: String

    override val moduleDescription: String
        get() = "SDK $sdkName"
}

/**
 * A sources for some [KtLibraryModule]
 */
public interface KtLibrarySourceModule : KtModule {
    public val libraryName: String

    /**
     * A library binary corresponding to the current library source.
     * If current module is a source JAR, then [binaryLibrary] is corresponds to the binaries JAR.
     */
    public val binaryLibrary: KtLibraryModule

    override val moduleDescription: String
        get() = "Library sourced of $libraryName"
}

/**
 * Module which contains kotlin [builtins](https://kotlinlang.org/spec/built-in-types-and-their-semantics.html) for specific platform
 * Kotlin builtins are usually reside in the compiler, so [contentScope] and [getBinaryRoots] are empty
 */
public class KtBuiltinsModule(
    override val platform: TargetPlatform,
    override val analyzerServices: PlatformDependentAnalyzerServices,
    override val project: Project
) : KtBinaryModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directRefinementDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()
    override val contentScope: GlobalSearchScope get() = GlobalSearchScope.EMPTY_SCOPE
    override fun getBinaryRoots(): Collection<Path> = emptyList()
    override val moduleDescription: String get() = "Builtins for $platform"

    override fun equals(other: Any?): Boolean = other is KtBuiltinsModule && this.platform == other.platform
    override fun hashCode(): Int = platform.hashCode()
}


/**
 * A set of sources which lives outside project content root. E.g, testdata files or source files of some other project.
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
