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
 * Injects java-direct into FIR JVM sessions via `createJavaFacade` when `useJavaDirect` is set.
 *
 * Dispatches by scope identity (`scope === librariesScope`): library sessions use
 * [LibraryJavaClassFinder] (package-info annotations only; binaries go through the deserializer);
 * source sessions use [JavaClassFinderOverAstImpl].
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
            scope === librariesScope -> LibraryJavaClassFinder(
                binaryClassFinderInputsBuilder(projectEnvironment, scope) as? JvmBinaryClassFinderInputsOverIndex
            )
            else -> JavaClassFinderOverAstImpl(session, sourceRootEntries)
        }
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

/**
 * Memoized [JvmBinaryClassFinderInputsOverIndex] for the library-session deserializer,
 * or `null` when no CLI `JvmDependenciesIndex` is available.
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
 * Library-session [JavaClassFinder]: all probes no-op except [findPackage], which exposes
 * binary `package-info.class` annotations for package default-nullability qualifiers.
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

/** [JavaPackage] carrying only binary `package-info.class` annotations. */
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
