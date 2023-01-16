/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import gnu.trove.THashSet
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
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
            classId.shortClassName.asString() + MetadataPackageFragment.DOT_METADATA_FILE_EXTENSION
        )?.inputStream
    }

    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> {
        val result = THashSet<String>()
        index.traverseDirectoriesInPackage(packageFqName, continueSearch = { dir, _ ->
            for (child in dir.children) {
                if (child.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION) {
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
            found = found or dir.children.any { it.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION }
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
