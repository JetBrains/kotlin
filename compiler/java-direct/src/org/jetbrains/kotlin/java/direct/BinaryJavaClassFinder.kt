/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.isNotTopLevelClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Index-based, PSI-free [JavaClassFinder] for binary `.class` (and optionally `.sig`) files.
 *
 * This is the **Phase 1 stepping stone** of the plan documented in
 * `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`. It replaces the
 * legacy PSI-based binary half of [CombinedJavaClassFinder] (`JavaClassFinderImpl`, instantiated
 * via `Project.createJavaClassFinder`) with a finder backed directly by the same
 * [JvmDependenciesIndex] that powers `CliVirtualFileFinder`. ASM-driven materialization is
 * delegated to the existing [BinaryJavaClass] reader from
 * `compiler/frontend.common.jvm/src/.../load/java/structure/impl/classFiles/`, the same one K1
 * has been using for years — no new `JavaClass` implementation is needed.
 *
 * Phase-1 design constraints (see §2.1 of the doc):
 *  - Observationally equivalent to PSI for the `FirJavaFacade.classFinder` contract:
 *    `findClass(classId)` resolves binary `JavaClass`es exactly as
 *    `KotlinCliJavaFileManagerImpl.findClass` does, including nested classes via
 *    `outerClass.findInnerClass(...)`.
 *  - Acts as a peer of `JvmClassFileBasedSymbolProvider` (also fed by the same
 *    [JvmDependenciesIndex] / `KotlinClassFinder`), not on top of it, to avoid the
 *    `FirJavaFacade ↔ deserializer` cycle described in §2.3.
 *
 * In Phase 2 this class is removed: binary lookups move into
 * `JvmClassFileBasedSymbolProvider` directly, `CombinedJavaClassFinder` disappears, and
 * `FirJavaFacade.classFinder` becomes source-only. **Designed for easy removal**, not for
 * long-term residence.
 *
 * @param index The same classpath index `CliVirtualFileFinder` uses for class/package lookups.
 * @param scope PSI search scope used to filter candidate `.class`/`.sig` virtual files; must
 *              match the scope the source-side `FirJavaFacade` was instantiated with.
 * @param enableSearchInCtSym Whether `.sig` (e.g. JDK `ct.sym`) entries should be consulted in
 *                            addition to plain `.class` files. Mirrors `CliVirtualFileFinder`'s
 *                            flag so JDK class visibility stays consistent across the pipeline.
 */
@Suppress("UnstableApiUsage")
class BinaryJavaClassFinder(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    private val enableSearchInCtSym: Boolean,
) : JavaClassFinder {

    private val extensions: JavaFileExtensions =
        if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS

    private val signatureParser = BinaryClassSignatureParser()

    /**
     * Memoization mirroring `KotlinCliJavaFileManagerImpl.binaryCache`. The scope is treated as
     * effectively constant per finder instance (one finder per `FirJavaFacade`), matching the
     * caching invariant of the legacy implementation — see the comment block in
     * `KotlinCliJavaFileManagerImpl.findClass`.
     */
    private val binaryCache: MutableMap<ClassId, JavaClass?> = HashMap()

    /**
     * Cache for the outer-most-class virtual file resolution. Identical role to
     * `KotlinCliJavaFileManagerImpl.topLevelClassesCache`. Two slots — one filtered by [scope],
     * the other unfiltered — because the resolver path needs `allScope` semantics (see
     * [resolver]).
     */
    private val topLevelClassesCache: MutableMap<FqName, VirtualFile?> = HashMap()
    private val topLevelClassesCacheAllScope: MutableMap<FqName, VirtualFile?> = HashMap()

    /**
     * Per-package known class-name cache. The index walk in
     * [knownClassNamesInPackage] is hot on the call path through
     * `FirJavaFacade.knownClassNamesInPackage`, so we keep a small per-finder memoization to
     * avoid traversing the index repeatedly for the same package. Sized like
     * `JavaPackageIndexer`'s caches in this module.
     */
    private val knownClassNamesCache: MutableMap<FqName, Set<String>> = HashMap()

    override fun findClass(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, applyScopeFilter = true)

    /**
     * Used by the per-call [ClassifierResolutionContext] to resolve cross-references read from
     * the bytecode signature (supertypes, parameter types, etc.). References from one binary
     * class to another must be resolvable across the **entire** classpath, not only within the
     * (potentially narrower) [scope] this finder was given for the current session. Mirrors the
     * `allScope` choice in `KotlinCliJavaFileManagerImpl.findClass` (cli-base), where the inner
     * [ClassifierResolutionContext] is constructed against `allScope` for the same reason.
     */
    private fun findClassWithoutScopeFilter(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, applyScopeFilter = false)

    private fun findClassImpl(request: JavaClassFinder.Request, applyScopeFilter: Boolean): JavaClass? {
        val (classId, classFileContentFromRequest, outerClassFromRequest) = request

        val outerMostClassFqName = classId.packageFqName.child(classId.relativeClassName.pathSegments().first())
        // Cache top-level lookups separately for the two scope modes so that a previously cached
        // null from a narrower session scope cannot mask a later all-scope hit (and vice versa).
        val topLevelCache = if (applyScopeFilter) topLevelClassesCache else topLevelClassesCacheAllScope
        val virtualFile = topLevelCache.getOrPut(outerMostClassFqName) {
            findTopLevelClassVirtualFile(outerMostClassFqName, applyScopeFilter)
        } ?: return null

        // The materialised `BinaryJavaClass` instances are independent of which finder mode
        // resolved them, so we share `binaryCache` for both modes.
        return binaryCache.getOrPut(classId) {
            val outerClassId = classId.outerClassId
            if (outerClassId != null) {
                val outerClass = outerClassFromRequest
                    ?: findClassImpl(JavaClassFinder.Request(outerClassId), applyScopeFilter)
                return@getOrPut if (outerClass is BinaryJavaClass) {
                    outerClass.findInnerClass(classId.shortClassName, classFileContentFromRequest)
                } else {
                    outerClass?.findInnerClass(classId.shortClassName)
                }
            }

            // Top-level class.
            val classContent = classFileContentFromRequest ?: virtualFile.contentsToByteArray()
            // Defensive: a class file whose name contains '$' but is actually nested must not be
            // returned as a top-level class. Mirrors the same guard in
            // `KotlinCliJavaFileManagerImpl.findClass`.
            if (virtualFile.nameWithoutExtension.contains("$") && isNotTopLevelClass(classContent)) {
                return@getOrPut null
            }

            // A fresh `ClassifierResolutionContext` is created per top-level [findClass] call
            // because the context is mutable: it accumulates type parameters and inner-class
            // info from every `BinaryJavaClass` it materialises. Sharing a single instance
            // across calls bleeds the type parameters of one class into the resolution of an
            // unrelated one (symptom: "Unresolved type for E" / wrong overload selection).
            // Mirrors `KotlinCliJavaFileManagerImpl.findClass` line 151.
            val resolver = ClassifierResolutionContext { ref ->
                findClassWithoutScopeFilter(JavaClassFinder.Request(ref))
            }

            BinaryJavaClass(
                virtualFile,
                classId.asSingleFqName(),
                resolver,
                signatureParser,
                outerClass = null,
                classContent = classContent,
            )
        }
    }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> {
        // Production FIR has no caller for the multi-result form against the binary side
        // (see §1.7 of the design doc). A single result is sufficient.
        return listOfNotNull(findClass(request))
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        var found = false
        index.traverseDirectoriesInPackage(fqName, JavaRoot.OnlyBinary) { _, _ ->
            found = true
            false // stop at the first hit
        }
        if (!found) return null
        // Binary packages have no `package-info.java` to read; this matches PSI's behaviour
        // for binary packages where annotations are not synthesised through this finder.
        return BinaryJavaPackage(fqName)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
        knownClassNamesCache.getOrPut(packageFqName) {
            val result = LinkedHashSet<String>()
            index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
                // Top-level classes only (consistent with the source-side finder and with
                // `CliVirtualFileFinder.findMetadataTopLevelClassesInPackage`). Inner-class
                // files (`Outer$Inner.class`) are loaded on demand via
                // `BinaryJavaClass.findInnerClass`, never enumerated here.
                val name = file.nameWithoutExtension
                if (!name.contains('$')) {
                    result.add(name)
                }
                true
            }
            result
        }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    private fun findTopLevelClassVirtualFile(
        outerMostClassFqName: FqName,
        applyScopeFilter: Boolean,
    ): VirtualFile? {
        val outerMostClassId = ClassId.topLevel(outerMostClassFqName)
        val candidates = index.findClassVirtualFiles(outerMostClassId, extensions)
        return if (applyScopeFilter) candidates.firstOrNull { it in scope } else candidates.firstOrNull()
    }

    private companion object {
        private val BINARY_CLASS_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS)
        private val BINARY_CLASS_AND_SIG_EXTENSIONS =
            JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
    }
}

/**
 * Minimal [JavaPackage] for binary packages discovered through [JvmDependenciesIndex].
 *
 * No annotations, sub-packages, or class enumeration are exposed: the only consumer in the FIR
 * pipeline is [org.jetbrains.kotlin.fir.java.FirJavaFacade.hasPackage], which checks for
 * non-`null`-ness only, and `FirJavaClass.javaPackage`, which reads `annotations` (lazy and
 * empty for binary packages here, matching PSI's behaviour for binary packages without
 * `package-info.class`).
 */
private class BinaryJavaPackage(override val fqName: FqName) : JavaPackage {
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override val subPackages: Collection<JavaPackage> get() = emptyList()
    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> = emptyList()
}
