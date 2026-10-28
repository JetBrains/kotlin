/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.cli.jvm.index.IndexedClassFile
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.asBinaryClassFileHandle
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.containsRoot
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.kotlin.HandleKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.InputStream

private val BINARY_CLASS_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS)
private val BINARY_CLASS_AND_SIG_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
private val JAVA_SOURCE_EXTENSIONS = JavaFileExtensions(JavaFileExtension.JAVA)
private val KOTLIN_METADATA_EXTENSIONS = JavaFileExtensions(JavaFileExtension.KOTLIN_METADATA)

/**
 * Finds Kotlin binary classes in [index], keeping only the files accepted by [accepts].
 *
 * Compiler code uses the [JvmClasspath] constructor, which accepts a file by the classpath root the index found it in and
 * reads Kotlin classes through [BinaryClassFileHandle]s ([HandleKotlinClass]). The file filter is an implementation seam
 * for hosts that select files in their own terms (e.g. Analysis API or K1 adapters over `GlobalSearchScope`); those keep
 * the [VirtualFile]-based Kotlin classes of [VirtualFileFinder], which K1 and the decompiler still inspect.
 */
class CliVirtualFileFinder private constructor(
    private val index: JvmDependenciesIndex,
    private val accepts: (IndexedClassFile) -> Boolean,
    private val enableSearchInCtSym: Boolean,
    private val kotlinClassesFromHandles: Boolean,
    private val perfManager: PerformanceManager?,
) : VirtualFileFinder(perfManager) {
    constructor(
        index: JvmDependenciesIndex,
        accepts: (VirtualFile) -> Boolean,
        enableSearchInCtSym: Boolean,
        perfManager: PerformanceManager?,
    ) : this(index, { accepts(it.file) }, enableSearchInCtSym, kotlinClassesFromHandles = false, perfManager)

    constructor(
        index: JvmDependenciesIndex,
        classpath: JvmClasspath,
        enableSearchInCtSym: Boolean,
        perfManager: PerformanceManager?,
    ) : this(index, { classpath.containsRoot(it.root.file) }, enableSearchInCtSym, kotlinClassesFromHandles = true, perfManager)

    // The two `VirtualFile` lookups stay for the backend (`inlineCodegenUtils.findVirtualFile`) and IDE/K1 code.
    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        findBinaryOrSigClass(classId)?.file

    override fun findSourceOrBinaryVirtualFile(classId: ClassId): VirtualFile? =
        findSourceOrBinaryClass(classId)?.file

    /** The `.class`/`.sig` file declaring [classId], if the classpath has one. */
    fun findBinaryClassFile(classId: ClassId): BinaryClassFileHandle? =
        findBinaryOrSigClass(classId)?.asBinaryClassFileHandle()

    /** [findBinaryClassFile], or else the `.java` file declaring [classId] in a Java source root. */
    fun findSourceOrBinaryClassFile(classId: ClassId): BinaryClassFileHandle? =
        findSourceOrBinaryClass(classId)?.asBinaryClassFileHandle()

    override fun findKotlinClassOrContent(classId: ClassId, metadataVersion: MetadataVersion): KotlinClassFinder.Result? {
        if (!kotlinClassesFromHandles) return super.findKotlinClassOrContent(classId, metadataVersion)
        val classFile = findBinaryOrSigClass(classId) ?: return null
        return readKotlinClass(classFile.asBinaryClassFileHandle(), metadataVersion)
    }

    override fun findKotlinClassOrContent(javaClass: JavaClass, metadataVersion: MetadataVersion): KotlinClassFinder.Result? {
        if (!kotlinClassesFromHandles || javaClass !is BinaryJavaClass) return super.findKotlinClassOrContent(javaClass, metadataVersion)
        // A nested class read from bytecode already holds its own class file (see `BinaryJavaClass.findInnerClass`).
        return readKotlinClass(javaClass.classFile, metadataVersion)
    }

    private fun readKotlinClass(classFile: BinaryClassFileHandle, metadataVersion: MetadataVersion): KotlinClassFinder.Result? {
        // Only a `.class` file may carry Kotlin metadata: a `.sig` file never does, and `module-info.class` is not a class.
        if (classFile.extension != JavaFileExtension.CLASS.extension) return null
        if (classFile.nameWithoutExtension == PsiJavaModule.MODULE_INFO_CLASS) return null
        return HandleKotlinClass.create(classFile, metadataVersion, fileContent = null, perfManager)
    }

    override fun findMetadata(classId: ClassId): InputStream? {
        assert(!classId.isNestedClass) { "Nested classes are not supported here: $classId" }

        return findClass(classId, KOTLIN_METADATA_EXTENSIONS)?.asBinaryClassFileHandle()?.readBytes()?.inputStream()
    }

    /** Not restricted by [accepts]: this lookup covers all indexed roots. */
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> {
        val result = ObjectOpenHashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, KOTLIN_METADATA_EXTENSIONS) { file ->
            result.add(file.nameWithoutExtension)
            true
        }
        return result
    }

    /** Not restricted by [accepts]: this lookup covers all indexed roots. */
    override fun hasMetadataPackage(fqName: FqName): Boolean {
        var found = false
        index.traverseClassVirtualFilesInPackage(fqName, KOTLIN_METADATA_EXTENSIONS) { _ ->
            found = true
            false
        }
        return found
    }

    /** Not restricted by [accepts]: this lookup covers all indexed roots. */
    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        // `.kotlin_builtins` files exist for a specific package and not for each class. As such, they don't have a class name. Because of
        // this, we use `traverseDirectoriesInPackage` and look for the file directly instead of using `findClassVirtualFiles`.
        val builtInsFileName = BuiltInSerializerProtocol.getBuiltInsFileName(packageFqName)
        var result: VirtualFile? = null
        index.traverseDirectoriesInPackage(packageFqName, acceptedRootTypes = JavaRoot.OnlyBinary) { directory, _ ->
            val file = directory.findChild(builtInsFileName)
            if (file != null && file.isValid) {
                result = file
                false // stop
            } else {
                true // continue
            }
        }
        return result?.inputStream
    }

    private fun findBinaryOrSigClass(classId: ClassId): IndexedClassFile? {
        val extensions = if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS
        return findClass(classId, extensions)
    }

    private fun findSourceOrBinaryClass(classId: ClassId): IndexedClassFile? =
        findBinaryOrSigClass(classId) ?: findClass(classId, JAVA_SOURCE_EXTENSIONS)

    private fun findClass(classId: ClassId, extensions: JavaFileExtensions): IndexedClassFile? =
        index.findClassFiles(classId, extensions).firstOrNull(accepts)
}
