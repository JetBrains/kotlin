/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DOT_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.InputStream

class CliVirtualFileFinder(
    private val index: JvmDependenciesIndex,
    private val scope: GlobalSearchScope,
    private val enableSearchInCtSym: Boolean
) : VirtualFileFinder() {
    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        findBinaryOrSigClass(classId)

    override fun findSourceOrBinaryVirtualFile(classId: ClassId) =
        findBinaryOrSigClass(classId)
            ?: findSourceClass(classId, classId.relativeClassName.asString() + ".java")

    override fun findMetadata(classId: ClassId): InputStream? {
        assert(!classId.isNestedClass) { "Nested classes are not supported here: $classId" }

        return findBinaryClass(
            classId,
            classId.shortClassName.asString() + DOT_METADATA_FILE_EXTENSION
        )?.inputStream
    }

    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> {
        val result = ObjectOpenHashSet<String>()
        index.traverseDirectoriesInPackage(packageFqName, continueSearch = { dir, _ ->
            for (child in dir.children) {
                if (child.extension == METADATA_FILE_EXTENSION) {
                    result.add(child.nameWithoutExtension)
                }
            }

            true
        })

        return result
    }

    override fun hasMetadataPackage(fqName: FqName): Boolean {
        var found = false
        index.traverseDirectoriesInPackage(fqName, continueSearch = { dir, _ ->
            found = found or dir.children.any { it.extension == METADATA_FILE_EXTENSION }
            !found
        })
        return found
    }

    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        // "<builtins-metadata>" is just a made-up name
        // JvmDependenciesIndex requires the ClassId of the class which we're searching for, to cache the last request+result
        val classId = ClassId(packageFqName, Name.special("<builtins-metadata>"))

        return findBinaryClass(classId, BuiltInSerializerProtocol.getBuiltInsFileName(packageFqName))?.inputStream
    }

    private fun findClass(classId: ClassId, fileName: String, rootType: Set<JavaRoot.RootType>) =
        index.findClass(classId, acceptedRootTypes = rootType) { dir, _ ->
            dir.findChild(fileName)?.takeIf(VirtualFile::isValid)
        }?.takeIf { it in scope }

    private fun findSigFileIfEnabled(
        dir: VirtualFile,
        simpleName: String
    ) = if (enableSearchInCtSym) dir.findChild("$simpleName.sig") else null

    private fun findBinaryOrSigClass(classId: ClassId, simpleName: String, rootType: Set<JavaRoot.RootType>) =
        index.findClass(classId, acceptedRootTypes = rootType) { dir, _ ->
            val file = dir.findChild("$simpleName.class") ?: findSigFileIfEnabled(dir, simpleName)
            if (file != null && file.isValid) file else null
        }?.takeIf { it in scope }

    private fun findBinaryOrSigClass(classId: ClassId) =
        findBinaryOrSigClass(classId, classId.relativeClassName.asString().replace('.', '$'), JavaRoot.OnlyBinary)

    private fun findBinaryClass(classId: ClassId, fileName: String) = findClass(classId, fileName, JavaRoot.OnlyBinary)
    private fun findSourceClass(classId: ClassId, fileName: String) = findClass(classId, fileName, JavaRoot.OnlySource)
}
