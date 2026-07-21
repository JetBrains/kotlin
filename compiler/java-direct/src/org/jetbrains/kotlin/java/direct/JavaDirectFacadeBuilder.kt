/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.asPsiSearchScope
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.java.deserialization.JvmBinaryClassFinderInputs
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Direct-injection seam that plugs `java-direct` into the FIR JVM sessions through the
 * `createJavaFacade` lambda on `FirJvmSessionFactory.createSourceSession` /
 * `createLibrarySession` (populated by `JvmFrontendPipelinePhase.prepareJvmSessions` when
 * `JvmAnalysisFlags.useJavaDirect` is set).
 *
 * The facade dispatches on the search scope:
 *  - **Library scope** (`scope === librariesScope`): a [LibraryJavaClassFinder] — binary classes
 *    are read by the deserializer through [JvmBinaryClassFinderInputsOverIndex]; the facade itself
 *    only surfaces the package-level annotations of a binary `package-info.class` (needed for
 *    default-nullability qualifiers read via `FirJavaClass.javaPackage`).
 *  - **Source scope**: a [JavaClassFinderOverAstImpl] over the configured Java source roots
 *    (with no roots the finder is effectively a no-op, so no special-casing is needed).
 *
 * Identity comparison is the correct dispatch key: the scope instances are constructed once in
 * `FirJvmSessionFactory.prepareSessions` and threaded through unchanged.
 */
fun createJavaDirectSourceJavaFacadeBuilder(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    librariesScope: AbstractProjectFileSearchScope,
    binaryClassFinderInputsBuilder: (AbstractProjectEnvironment, AbstractProjectFileSearchScope) -> JvmBinaryClassFinderInputs?,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val sourceRootEntries: List<JavaSourceRootEntry> =
        configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).asSequence()
            .filterIsInstance<JavaSourceRoot>()
            .map { javaRoot ->
                val prefix =
                    if (javaRoot.packagePrefix.isNullOrEmpty()) FqName.ROOT
                    else FqName(javaRoot.packagePrefix!!)
                JavaSourceRootEntry(javaRoot.file, prefix)
            }
            .toList()

    return { _, session, moduleData, scope ->
        val finder: JavaClassFinder = when {
            // Library-session facade: class lookups go through the deserializer; the facade only
            // reads binary `package-info.class` package annotations (see file KDoc). Reuse the
            // same (memoised) binary index the deserializer reads through, so the two stay in
            // sync on scope and ct.sym visibility.
            scope === librariesScope -> LibraryJavaClassFinder(
                binaryClassFinderInputsBuilder(projectEnvironment, scope) as? JvmBinaryClassFinderInputsOverIndex
            )
            // Source-session facade: source-only `JavaClassFinderOverAstImpl`. With no Java
            // sources the AST finder is effectively a no-op over an empty index.
            else -> JavaClassFinderOverAstImpl(session, sourceRootEntries)
        }
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

/**
 * Companion to [createJavaDirectSourceJavaFacadeBuilder]: produces the deserializer-side
 * [JvmBinaryClassFinderInputs] lambda for `FirJvmSessionFactory.createLibrarySession`. On the
 * CLI path it returns a [JvmBinaryClassFinderInputsOverIndex] over the same index
 * `CliVirtualFileFinder` uses, memoised per `(scope identityHash, enableCtSym)`; in non-CLI
 * environments (no `JvmDependenciesIndex`) it returns `null` and the deserializer falls back to
 * `FirJavaFacade`.
 */
@Suppress("UnstableApiUsage")
fun createJavaDirectBinaryClassFinderInputsBuilder(
    projectEnvironment: VfsBasedProjectEnvironment,
): (AbstractProjectEnvironment, AbstractProjectFileSearchScope) -> JvmBinaryClassFinderInputs? {
    val cache: MutableMap<BinaryInputsCacheKey, JvmBinaryClassFinderInputs?> = HashMap()
    return { _, scope ->
        val psiSearchScope: GlobalSearchScope = scope.asPsiSearchScope()
        val vfff = VirtualFileFinderFactory.getInstance(projectEnvironment.project) as? CliVirtualFileFinderFactory
        val key = BinaryInputsCacheKey(System.identityHashCode(psiSearchScope), vfff?.enableSearchInCtSym)
        cache.getOrPut(key) {
            if (vfff != null) {
                JvmBinaryClassFinderInputsOverIndex(vfff.index, psiSearchScope, vfff.enableSearchInCtSym)
            } else {
                null
            }
        }
    }
}

private data class BinaryInputsCacheKey(val scopeIdentity: Int, val enableCtSym: Boolean?)

/**
 * [JavaClassFinder] for the library-session facade. Binary class/package existence and lookup are
 * handled by the deserializer through [JvmBinaryClassFinderInputsOverIndex], so every probe here
 * is a no-op for which `null`/`false`/empty is the correct deserializer-fallback outcome — except
 * [findPackage], which surfaces the annotations of a binary `package-info.class` so package-level
 * default-nullability qualifiers (`@TypeQualifierDefault`, JSpecify `@NullMarked`, …) are applied
 * to binary classes just as PSI applies them.
 *
 * @param binaryInputs the binary index adapter for the library scope, or `null` in non-CLI
 *   environments with no [JvmBinaryClassFinderInputsOverIndex] (then no package annotations are
 *   available and [findPackage] returns `null`, matching the previous behaviour).
 */
private class LibraryJavaClassFinder(
    private val binaryInputs: JvmBinaryClassFinderInputsOverIndex?,
) : JavaClassFinder {
    override fun findClass(request: JavaClassFinder.Request): JavaClass? = null

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> = emptyList()

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        if (!mayHaveAnnotations) return null
        val packageInfoClass = binaryInputs?.findPackageInfoClass(fqName) ?: return null
        return BinaryPackageInfoJavaPackage(fqName, packageInfoClass)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = null

    override fun canComputeKnownClassNamesInPackage(): Boolean = false

    override fun isInSourceIndex(classId: ClassId): Boolean = false

    override fun hasPackageInSources(fqName: FqName): Boolean = false

    override fun sourceClassNamesInPackage(packageFqName: FqName): Set<String>? = null
}

/**
 * A [JavaPackage] that carries only the annotations of a binary `package-info.class`. On the
 * library-session path this is reached solely via `FirJavaFacade`'s package cache to read the
 * package's default-nullability qualifiers; class and sub-package enumeration is served by the
 * deserializer, so those stay empty.
 */
private class BinaryPackageInfoJavaPackage(
    override val fqName: FqName,
    private val packageInfoClass: JavaClass,
) : JavaPackage {
    override val annotations: Collection<JavaAnnotation>
        get() = packageInfoClass.annotations

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val subPackages: Collection<JavaPackage>
        get() = emptyList()

    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> = emptyList()
}
