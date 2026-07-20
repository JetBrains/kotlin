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
import org.jetbrains.kotlin.fir.java.deserialization.JvmBinaryClassFinderInputs
import org.jetbrains.kotlin.fir.java.hasMetadataAnnotation
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.isNotTopLevelClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Index-based, PSI-free implementation of [JvmBinaryClassFinderInputs] for binary `.class`
 * (and optionally `.sig`) files on the `java-direct` library session — the single binary-side
 * entry point the deserializer reads through instead of routing via `FirJavaFacade`. ASM-driven
 * materialization is delegated to [BinaryJavaClass]. [findBinaryClass] filters out Kotlin
 * classes carrying `@Metadata` — those are handled by the Kotlin branch of
 * `extractClassMetadata`.
 *
 * @param index The same classpath index `CliVirtualFileFinder` uses for class/package lookups.
 * @param scope PSI search scope used to filter candidate `.class`/`.sig` virtual files; must
 *              match the scope the deserializer's library session was instantiated with.
 * @param enableSearchInCtSym Whether `.sig` (e.g. JDK `ct.sym`) entries should be consulted in
 *                            addition to plain `.class` files. Mirrors `CliVirtualFileFinder`'s
 *                            flag so JDK class visibility stays consistent across the pipeline.
 */
@Suppress("UnstableApiUsage")
class JvmBinaryClassFinderInputsOverIndex(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    private val enableSearchInCtSym: Boolean,
) : JvmBinaryClassFinderInputs {

    private val extensions: JavaFileExtensions =
        if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS

    private val signatureParser = BinaryClassSignatureParser()

    /**
     * Memoizes resolved binary classes. The scope is effectively constant per instance (one
     * finder per library session).
     */
    private val binaryCache: MutableMap<ClassId, JavaClass?> = HashMap()

    /**
     * Caches outer-most-class virtual file resolution. Two slots — one filtered by [scope], the
     * other unfiltered — because the per-call [ClassifierResolutionContext] is constructed
     * against `allScope` semantics for cross-references read from the bytecode signature.
     */
    private val topLevelClassesCache: MutableMap<FqName, VirtualFile?> = HashMap()
    private val topLevelClassesCacheAllScope: MutableMap<FqName, VirtualFile?> = HashMap()

    /**
     * Per-package known class-name cache. The index walk is hot on the path through
     * [knownBinaryClassNamesInPackage] and [hasTopLevelBinaryClass], so memoize per package to
     * avoid traversing the index repeatedly.
     */
    private val knownClassNamesCache: MutableMap<FqName, Set<String>> = HashMap()

    override fun hasTopLevelBinaryClass(classId: ClassId): Boolean {
        // Check the outermost-class name against the known names of the package
        // (`FqName.topLevelName()` is `asString().substringBefore(".")`, inlined here).
        val knownNames = knownClassNamesInPackage(classId.packageFqName)
        val topLevelName = classId.relativeClassName.asString().substringBefore(".")
        return topLevelName in knownNames
    }

    override fun knownBinaryClassNamesInPackage(packageFqName: FqName): Set<String> =
        knownClassNamesInPackage(packageFqName)

    override fun hasBinaryPackage(fqName: FqName): Boolean {
        var found = false
        index.traverseDirectoriesInPackage(fqName, JavaRoot.OnlyBinary) { _, _ ->
            found = true
            false // stop at the first hit
        }
        return found
    }

    override fun findBinaryClass(classId: ClassId, knownContent: ByteArray?): JavaClass? =
        // Filter out Kotlin classes carrying `@Metadata` (see class KDoc).
        findClassImpl(JavaClassFinder.Request(classId, knownContent), applyScopeFilter = true)
            ?.takeIf { it.isFromSource || !it.hasMetadataAnnotation() }

    /**
     * Materialises `<package>/package-info.class` when it is present in [scope], so its
     * class-level annotations can be surfaced as the package's default-nullability qualifiers
     * (`@TypeQualifierDefault` / JSpecify `@NullMarked` etc.). Returns `null` when the package
     * carries no `package-info` on the binary classpath. The library facade routes binary
     * class/package existence through the deserializer, so this is the only package-level
     * information the facade needs from the binary side.
     */
    fun findPackageInfoClass(packageFqName: FqName): JavaClass? =
        findBinaryClass(ClassId(packageFqName, Name.identifier("package-info")), knownContent = null)

    private fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
        knownClassNamesCache.getOrPut(packageFqName) {
            val result = LinkedHashSet<String>()
            index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
                // Include every class file's name, including ones that contain `$`. Genuine
                // inner-class spill (`Outer$Inner.class`) is filtered later inside [findClassImpl]
                // via `isNotTopLevelClass(classContent)`. A blanket name-level `$` filter wrongly
                // hides legitimate top-level classes whose JVM name contains `$` — e.g. Scala
                // companion modules (`Foo$.class`) — which Kotlin imports via backticks.
                result.add(file.nameWithoutExtension)
                true
            }
            result
        }

    /**
     * Used by the per-call [ClassifierResolutionContext] to resolve cross-references read from
     * the bytecode signature (supertypes, parameter types, etc.). References from one binary
     * class to another must be resolvable across the **entire** classpath, not only within the
     * (potentially narrower) [scope] this finder was given for the current session.
     */
    private fun findClassWithoutScopeFilter(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, applyScopeFilter = false)

    private fun findClassImpl(request: JavaClassFinder.Request, applyScopeFilter: Boolean): JavaClass? {
        val [classId, classFileContentFromRequest, outerClassFromRequest] = request

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
            // returned as a top-level class.
            if (virtualFile.nameWithoutExtension.contains("$") && isNotTopLevelClass(classContent)) {
                return@getOrPut null
            }

            // A fresh `ClassifierResolutionContext` is created per top-level [findClassImpl] call
            // because the context is mutable: it accumulates type parameters and inner-class
            // info from every `BinaryJavaClass` it materialises. Sharing a single instance
            // across calls bleeds the type parameters of one class into the resolution of an
            // unrelated one (symptom: "Unresolved type for E" / wrong overload selection).
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
