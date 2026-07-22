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
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.readBinaryJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Index-based [JvmBinaryClassFinderInputs] for the java-direct library session.
 * Skips Kotlin `@Metadata` classes (handled by the Kotlin deserializer path).
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

    private val binaryCache: MutableMap<ClassId, JavaClass?> = HashMap()

    // Separate caches so a scoped miss cannot mask a later all-scope hit (and vice versa).
    private val topLevelClassesCache: MutableMap<FqName, VirtualFile?> = HashMap()
    private val topLevelClassesCacheAllScope: MutableMap<FqName, VirtualFile?> = HashMap()

    private val knownClassNamesCache: MutableMap<FqName, Set<String>> = HashMap()

    override fun hasTopLevelBinaryClass(classId: ClassId): Boolean {
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
        findClassImpl(JavaClassFinder.Request(classId, knownContent), applyScopeFilter = true)
            ?.takeIf { it.isFromSource || !it.hasMetadataAnnotation() }

    /** Binary `package-info.class` for package default-nullability annotations, if present. */
    fun findPackageInfoClass(packageFqName: FqName): JavaClass? =
        findBinaryClass(ClassId(packageFqName, Name.identifier("package-info")), knownContent = null)

    private fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
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

    /** Cross-references from bytecode must resolve against the full classpath, not only [scope]. */
    private fun findClassWithoutScopeFilter(request: JavaClassFinder.Request): JavaClass? =
        findClassImpl(request, applyScopeFilter = false)

    private fun findClassImpl(request: JavaClassFinder.Request, applyScopeFilter: Boolean): JavaClass? {
        val [classId, classFileContentFromRequest, outerClassFromRequest] = request

        val outerMostClassFqName = classId.packageFqName.child(classId.relativeClassName.pathSegments().first())
        val topLevelCache = if (applyScopeFilter) topLevelClassesCache else topLevelClassesCacheAllScope
        val virtualFile = topLevelCache.getOrPut(outerMostClassFqName) {
            findTopLevelClassVirtualFile(outerMostClassFqName, applyScopeFilter)
        } ?: return null

        // binaryCache is shared across scope modes; cross-refs use the unscoped resolver.
        return readBinaryJavaClass(
            classId = classId,
            topLevelVirtualFile = virtualFile,
            classFileContent = classFileContentFromRequest,
            outerClassFromRequest = outerClassFromRequest,
            binaryCache = binaryCache,
            signatureParser = signatureParser,
            findOuterClass = { outerClassId -> findClassImpl(JavaClassFinder.Request(outerClassId), applyScopeFilter) },
            resolveCrossReference = { ref -> findClassWithoutScopeFilter(JavaClassFinder.Request(ref)) },
        )
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
