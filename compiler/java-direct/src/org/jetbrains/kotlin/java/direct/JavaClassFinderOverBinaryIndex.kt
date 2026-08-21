/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.readBinaryJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Binary-side [JavaClassFinder] over the CLI [JvmDependenciesIndex], used by the java-direct library session.
 *
 * [scope] is the part of the binary classpath of the compilation this session may see. It is different from the whole classpath
 * during an incremental compilation only: the output directory of the previous build is the scope of the precompiled-binaries
 * session, and is subtracted from the scope of the libraries session. What that shape decides in a compilation which runs
 * it — including that a reference recorded in a class file is resolved outside the scope its reader may see — is pinned
 * on the PSI peer of this finder by `org.jetbrains.kotlin.incremental.IncrementalJavaClassFromPreviousOutputTest`.
 */
class JavaClassFinderOverBinaryIndex(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    enableSearchInCtSym: Boolean,
) : JavaClassFinder {

    private val extensions: JavaFileExtensions =
        if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS

    private val signatureParser = BinaryClassSignatureParser()

    private val binaryCache: MutableMap<ClassId, JavaClass?> = HashMap()

    private val topLevelClassFiles: MutableMap<FqName, MutableMap<Name, Collection<VirtualFile>>> = HashMap()

    private val knownClassNamesCache: MutableMap<FqName, Set<String>> = HashMap()

    override fun findClass(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, visibleScope = scope)

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> =
        listOfNotNull(findClass(request))

    /**
     * A package exists on the binary side if some classpath root contains the corresponding
     * directory. Its annotations, if any, come from `package-info.class` and are only looked up
     * when the caller expects them (see `FirJavaFacade.packageCache`).
     */
    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        val packageInfoClass = if (mayHaveAnnotations) findPackageInfoClass(fqName) else null
        if (packageInfoClass == null && !containsDirectory(fqName)) return null
        return BinaryIndexJavaPackage(fqName, packageInfoClass)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
        knownClassNamesCache.getOrPut(packageFqName) {
            val result = LinkedHashSet<String>()
            index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
                // Keep names with `$` (e.g. Scala `Foo$`); real inner classes are filtered later
                // via isNotTopLevelClass on class content.
                result.add(file.nameWithoutExtension)
                true
            }
            result
        }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    /** Binary `package-info.class` for package default-nullability annotations, if present. */
    private fun findPackageInfoClass(packageFqName: FqName): JavaClass? =
        findClass(JavaClassFinder.Request(ClassId(packageFqName, PACKAGE_INFO_NAME)))

    private fun containsDirectory(fqName: FqName): Boolean {
        var found = false
        index.traverseDirectoriesInPackage(fqName, JavaRoot.OnlyBinary) { _, _ ->
            found = true
            false // stop at the first hit
        }
        return found
    }

    private fun findClassImpl(request: JavaClassFinder.Request, visibleScope: GlobalSearchScope): JavaClass? {
        val [classId, classFileContentFromRequest, outerClassFromRequest] = request

        val candidates = findTopLevelClassFiles(classId.packageFqName, classId.relativeClassName.topLevelName())
        val virtualFile = candidates.firstOrNull { it in visibleScope } ?: return null

        return readBinaryJavaClass(
            classId = classId,
            topLevelVirtualFile = virtualFile,
            classFileContent = classFileContentFromRequest,
            outerClassFromRequest = outerClassFromRequest,
            binaryCache = binaryCache,
            signatureParser = signatureParser,
            findOuterClass = { outerClassId -> findClassImpl(JavaClassFinder.Request(outerClassId), visibleScope) },
            resolveCrossReference = { ref -> findClassImpl(JavaClassFinder.Request(ref), EverythingGlobalScope()) },
        )
    }

    // Indexed by the two parts of the outermost class name as they already exist in a `ClassId`. An `FqName`
    // of that class would be a nicer single key, but building it costs a string concatenation, an `FqName`,
    // an `FqNameUnsafe`, a `pathSegments()` list and a hash of a fresh string on every lookup.
    private fun findTopLevelClassFiles(packageFqName: FqName, topLevelName: Name): Collection<VirtualFile> =
        topLevelClassFiles.getOrPut(packageFqName) { HashMap() }.getOrPut(topLevelName) {
            index.findClassVirtualFiles(ClassId(packageFqName, topLevelName), extensions)
        }

    private companion object {
        private val PACKAGE_INFO_NAME = Name.identifier("package-info")
        private val BINARY_CLASS_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS)
        private val BINARY_CLASS_AND_SIG_EXTENSIONS =
            JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
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

    @K1Deprecation
    override val subPackages: Collection<JavaPackage>
        get() = emptyList()

    @K1Deprecation
    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> = emptyList()
}
