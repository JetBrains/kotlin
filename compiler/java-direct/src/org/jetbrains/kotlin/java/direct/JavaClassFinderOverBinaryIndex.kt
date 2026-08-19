/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.readBinaryJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Binary-side [JavaClassFinder] of a session: the classpath-wide [BinaryJavaClassCache] restricted to the
 * [classpath] of that session. Kotlin `@Metadata` classes are filtered out by
 * [org.jetbrains.kotlin.fir.java.FirJavaFacade.findClass].
 */
class JavaClassFinderOverBinaryIndex(
    private val classes: BinaryJavaClassCache,
    private val classpath: JvmClasspath,
) : JavaClassFinder {

    override fun findClass(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, visibleClasspath = classpath)

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> =
        listOfNotNull(findClass(request))

    /**
     * A package exists on the binary side if some classpath root contains the corresponding
     * directory. Its annotations, if any, come from `package-info.class` and are only looked up
     * when the caller expects them (see `FirJavaFacade.packageCache`).
     */
    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        val packageInfoClass = if (mayHaveAnnotations) findPackageInfoClass(fqName) else null
        if (packageInfoClass == null && !classes.containsPackageDirectory(fqName)) return null
        return BinaryIndexJavaPackage(fqName, packageInfoClass)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
        classes.classFileNamesInPackage(packageFqName)

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    /** Binary `package-info.class` for package default-nullability annotations, if present. */
    private fun findPackageInfoClass(packageFqName: FqName): JavaClass? =
        findClass(JavaClassFinder.Request(ClassId(packageFqName, PACKAGE_INFO_NAME)))

    private fun findClassImpl(request: JavaClassFinder.Request, visibleClasspath: JvmClasspath): JavaClass? {
        val [classId, classFileContentFromRequest, outerClassFromRequest] = request

        val candidates = classes.findTopLevelClassFiles(classId.packageFqName, classId.relativeClassName.topLevelName())
        val classFile = candidates.firstOrNull { it in visibleClasspath } ?: return null

        return readBinaryJavaClass(
            classId = classId,
            topLevelClassFile = classFile,
            classFileContent = classFileContentFromRequest,
            outerClassFromRequest = outerClassFromRequest,
            binaryCache = classes.classes,
            signatureParser = classes.signatureParser,
            findOuterClass = { outerClassId -> findClassImpl(JavaClassFinder.Request(outerClassId), visibleClasspath) },
            resolveCrossReference = { ref -> findClassImpl(JavaClassFinder.Request(ref), JvmClasspath.ProjectLibraries()) },
        )
    }

    private companion object {
        private val PACKAGE_INFO_NAME = Name.identifier("package-info")
    }
}

/**
 * Whether [classFile] is on [this] part of the classpath, i.e. lies in one of its roots.
 *
 * The class files come from a [org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileIndex], which
 * is the binary classpath of the *whole* compilation, so every one of them is on that classpath by construction:
 * [JvmClasspath.ProjectLibraries] therefore has nothing to test but its exclusions.
 */
internal operator fun JvmClasspath.contains(classFile: BinaryClassFileHandle): Boolean = when (this) {
    is JvmClasspath.Roots -> roots.any(classFile::isUnder)
    is JvmClasspath.ProjectLibraries -> excludedRoots.none(classFile::isUnder)
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
