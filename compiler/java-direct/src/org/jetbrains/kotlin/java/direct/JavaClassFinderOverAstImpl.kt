/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.java.direct.model.JavaPackageOverAst
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.LeanJavaClassFinder
import org.jetbrains.kotlin.java.direct.util.DefaultJavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.JavaSupertypeGraph
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * [JavaClassFinder] implementation backed by the direct Java AST parser in this module.
 *
 * Acts as a thin orchestrator over three focused collaborators:
 *  - [JavaPackageIndexer] — lazy directory walk, per-package `className → files` index.
 *  - [JavaPackageInfoIndexer] — `package-info.java` parsing and package-annotation aggregation.
 *  - [JavaClassCache] — `ClassId → JavaClass` memoization and on-demand file parsing.
 *
 * This class owns the [JavaClassFinder] / [LeanJavaClassFinder] contract and the
 * [JavaSupertypeGraph] used for inherited-inner-class resolution. All the state that is specific
 * to a single concern lives on the corresponding collaborator.
 */
class JavaClassFinderOverAstImpl internal constructor(
    sourceRootEntries: List<JavaSourceRootEntry>,
    sourceFileReader: JavaSourceFileReader = DefaultJavaSourceFileReader,
) : JavaClassFinder, LeanJavaClassFinder {

    private val packageInfoIndexer = JavaPackageInfoIndexer(
        sourceFileReader = sourceFileReader,
        resolutionContextFactory = { tree -> JavaResolutionContext.create(tree, classFinder = this) },
    )

    private val packageIndexer = JavaPackageIndexer(sourceRootEntries, sourceFileReader, packageInfoIndexer)

    private val classCache = JavaClassCache(
        sourceFileReader = sourceFileReader,
        resolutionContextFactory = { tree -> JavaResolutionContext.create(tree, classFinder = this) },
    )

    private val supertypeGraph = JavaSupertypeGraph(
        classCacheLookup = { classCache[it] },
        filesForClassLookup = { classId -> packageIndexer.findFilesForClass(classId).map { it.file } },
        sameClassInSameFilePackage = { pkg, name -> packageIndexer.ensurePackageIndexed(pkg).containsKey(name) },
        sourceFileReader = sourceFileReader,
    )

    override fun isClassInIndex(classId: ClassId): Boolean {
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString() ?: return false
        return packageIndexer.ensurePackageIndexed(classId.packageFqName).containsKey(topLevelName)
    }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? =
        classCache.getOrPutIfNotNull(request.classId) { findClasses(request).firstOrNull() }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> {
        val classId = request.classId
        val segments = classId.relativeClassName.pathSegments().map(Name::asString)
        if (segments.isEmpty()) return emptyList()
        val topLevelName = segments.first()
        val innerNames = segments.drop(1)

        val classesByName = packageIndexer.ensurePackageIndexed(classId.packageFqName)
        val candidates = classesByName[topLevelName] ?: return emptyList()

        val result = mutableListOf<JavaClass>()
        for (file in candidates) {
            val javaClass = classCache.parseTopLevelClassFromFile(file, topLevelName) ?: continue
            val resolved = if (innerNames.isEmpty()) {
                javaClass
            } else {
                var cur: JavaClass? = javaClass
                for (name in innerNames) {
                    cur = cur?.findInnerClass(Name.identifier(name))
                    if (cur == null) break
                }
                cur
            }
            if (resolved != null) result.add(resolved)
        }
        return result
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        // Recognise three cases as "package exists":
        //  1. The package directly contains `.java` source files;
        //  2. A `package-info.java` carries package-level annotations (no other classes);
        //  3. The package is an ancestor of an indexed Java source file — e.g. a single file at
        //     `priv/members/check/Foo.java` makes `priv` and `priv.members` valid Java packages
        //     too, matching PSI's [org.jetbrains.kotlin.load.java.JavaClassFinderImpl.findPackage]
        //     behaviour. Required for source parity once [BinaryJavaClassFinder] is the binary
        //     half (it only consults binary roots and cannot see source-only ancestor packages),
        //     otherwise dotted FQN references and star imports across non-direct ancestors fail
        //     to resolve.
        val classesByName = packageIndexer.ensurePackageIndexed(fqName)
        if (classesByName.isNotEmpty()) return JavaPackageOverAst(fqName, this)
        if (packageInfoIndexer.hasPackageAnnotations(fqName)) return JavaPackageOverAst(fqName, this)
        if (packageIndexer.containsPackage(fqName)) return JavaPackageOverAst(fqName, this)
        return null
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
        packageIndexer.knownClassNamesInPackage(packageFqName)

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    override fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> =
        supertypeGraph.collectInheritedInnerClasses(classId)

    // ---- Internal API used by JavaPackageOverAst ----

    internal fun getPackageAnnotations(packageFqName: FqName): List<JavaAnnotation> {
        // Package-info files are parsed as a side effect of package indexing; trigger it first.
        packageIndexer.ensurePackageIndexed(packageFqName)
        return packageInfoIndexer.getPackageAnnotations(packageFqName)
    }

    internal fun classesInPackage(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<JavaClass> {
        val classesByName = packageIndexer.ensurePackageIndexed(fqName)
        if (classesByName.isEmpty()) return emptyList()
        val result = mutableListOf<JavaClass>()
        for ((simpleName, files) in classesByName) {
            val name = Name.identifier(simpleName)
            if (!nameFilter(name)) continue
            for (file in files) {
                classCache.parseTopLevelClassFromFile(file, simpleName)?.let { result.add(it) }
            }
        }
        return result
    }

    internal fun subPackagesOf(fqName: FqName): Collection<FqName> =
        packageIndexer.subPackagesOf(fqName)

    internal fun getDirectSupertypes(classId: ClassId): List<ClassId> =
        supertypeGraph.getDirectSupertypes(classId)
}
