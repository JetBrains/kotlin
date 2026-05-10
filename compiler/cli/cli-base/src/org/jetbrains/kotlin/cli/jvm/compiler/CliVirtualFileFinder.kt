/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.InputStream

private val BINARY_CLASS_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS)
private val BINARY_CLASS_AND_SIG_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
private val JAVA_SOURCE_EXTENSIONS = JavaFileExtensions(JavaFileExtension.JAVA)
private val KOTLIN_METADATA_EXTENSIONS = JavaFileExtensions(JavaFileExtension.KOTLIN_METADATA)

class CliVirtualFileFinder(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    private val enableSearchInCtSym: Boolean,
    perfManager: PerformanceManager?,
) : VirtualFileFinder(perfManager) {
    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        findBinaryOrSigClass(classId)

    override fun findSourceOrBinaryVirtualFile(classId: ClassId) =
        findBinaryOrSigClass(classId)
            ?: findClass(classId, JAVA_SOURCE_EXTENSIONS)

    override fun findMetadata(classId: ClassId): InputStream? {
        assert(!classId.isNestedClass) { "Nested classes are not supported here: $classId" }

        return findClass(classId, KOTLIN_METADATA_EXTENSIONS)?.inputStream
    }

    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> {
        val result = ObjectOpenHashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, KOTLIN_METADATA_EXTENSIONS) { file ->
            result.add(file.nameWithoutExtension)
            true
        }
        return result
    }

    override fun hasMetadataPackage(fqName: FqName): Boolean {
        var found = false
        index.traverseClassVirtualFilesInPackage(fqName, KOTLIN_METADATA_EXTENSIONS) { _ ->
            found = true
            false
        }
        return found
    }

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

    private fun findBinaryOrSigClass(classId: ClassId): VirtualFile? {
        val extensions = if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS
        return findClass(classId, extensions)
    }

    private fun findClass(classId: ClassId, extensions: JavaFileExtensions) =
        index.findClassVirtualFiles(classId, extensions).firstOrNull { it in scope }
}
