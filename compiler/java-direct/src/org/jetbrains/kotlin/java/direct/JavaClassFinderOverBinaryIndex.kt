/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassRoots
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.TopLevelClassFileCandidates
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.readBinaryJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Binary-side [JavaClassFinder] over the [BinaryClassRoots] of one session, used by the java-direct
 * library session. Kotlin `@Metadata` classes are filtered out by
 * [org.jetbrains.kotlin.fir.java.FirJavaFacade.findClass], which owns every read of this finder.
 */
class JavaClassFinderOverBinaryIndex(
    private val roots: BinaryClassRoots,
) : JavaClassFinder {

    private val signatureParser = BinaryClassSignatureParser()

    private val binaryCache: MutableMap<ClassId, JavaClass?> = HashMap()

    private val topLevelClassesCache: MutableMap<FqName, MutableMap<Name, TopLevelClassFileCandidates>> = HashMap()

    private val knownClassNamesCache: MutableMap<FqName, Set<String>> = HashMap()

    override fun findClass(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, applyScopeFilter = true)

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> =
        listOfNotNull(findClass(request))

    /**
     * A package exists on the binary side if some classpath root contains the corresponding
     * directory. Its annotations, if any, come from `package-info.class` and are only looked up
     * when the caller expects them (see `FirJavaFacade.packageCache`).
     */
    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        val packageInfoClass = if (mayHaveAnnotations) findPackageInfoClass(fqName) else null
        if (packageInfoClass == null && !roots.containsPackageDirectory(fqName)) return null
        return BinaryIndexJavaPackage(fqName, packageInfoClass)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
        knownClassNamesCache.getOrPut(packageFqName) { roots.classFileNamesInPackage(packageFqName) }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    /** Binary `package-info.class` for package default-nullability annotations, if present. */
    private fun findPackageInfoClass(packageFqName: FqName): JavaClass? =
        findClass(JavaClassFinder.Request(ClassId(packageFqName, PACKAGE_INFO_NAME)))

    /** Cross-references from bytecode must resolve against the full classpath, not only this session's scope. */
    private fun findClassWithoutScopeFilter(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, applyScopeFilter = false)

    private fun findClassImpl(request: JavaClassFinder.Request, applyScopeFilter: Boolean): JavaClass? {
        val [classId, classFileContentFromRequest, outerClassFromRequest] = request

        // Keyed by the two parts of the outermost class name as they already exist in `classId`.
        // An `FqName` of that class would be a nicer single key, but building it costs a string
        // concatenation, an `FqName`, an `FqNameUnsafe`, a `pathSegments()` list and a hash of a
        // fresh string on *every* lookup, and lookups outnumber misses ~7:1 (see
        // `implDocs/archive/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md` §12).
        val packageFqName = classId.packageFqName
        val topLevelName = classId.relativeClassName.topLevelName()
        val topLevelClassFiles = topLevelClassesCache.getOrPut(packageFqName) { HashMap() }.getOrPut(topLevelName) {
            roots.findTopLevelClassFiles(ClassId(packageFqName, topLevelName))
        }
        val classFile = (if (applyScopeFilter) topLevelClassFiles.inScope else topLevelClassFiles.anywhere) ?: return null

        // binaryCache is shared across scope modes; cross-refs use the unscoped resolver.
        return readBinaryJavaClass(
            classId = classId,
            topLevelClassFile = classFile,
            classFileContent = classFileContentFromRequest,
            outerClassFromRequest = outerClassFromRequest,
            binaryCache = binaryCache,
            signatureParser = signatureParser,
            findOuterClass = { outerClassId -> findClassImpl(JavaClassFinder.Request(outerClassId), applyScopeFilter) },
            resolveCrossReference = { ref -> findClassWithoutScopeFilter(JavaClassFinder.Request(ref)) },
        )
    }

    private companion object {
        private val PACKAGE_INFO_NAME = Name.identifier("package-info")
    }
}

/**
 * The name of the outermost class of a [ClassId.relativeClassName], without building an [FqName]:
 * `Outer.Inner.Nested` -> `Outer`. Reuses the already computed short name in the top-level case,
 * which is the vast majority of the requests.
 */
private fun FqName.topLevelName(): Name {
    val relativeClassName = asString()
    val firstDot = relativeClassName.indexOf('.')
    return if (firstDot < 0) shortName() else Name.identifier(relativeClassName.substring(0, firstDot))
}

/** [JavaPackage] carrying only the binary `package-info.class` annotations, if any. */
private class BinaryIndexJavaPackage(
    override val fqName: FqName,
    private val packageInfoClass: JavaClass?,
) : JavaPackage {
    override val annotations: Collection<JavaAnnotation>
        get() = packageInfoClass?.annotations.orEmpty()

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val subPackages: Collection<JavaPackage>
        get() = emptyList()

    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> = emptyList()
}
