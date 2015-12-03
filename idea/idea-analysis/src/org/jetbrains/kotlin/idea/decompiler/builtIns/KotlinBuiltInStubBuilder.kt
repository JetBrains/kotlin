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

package org.jetbrains.kotlin.idea.decompiler.builtIns

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsSerializedResourcePaths
import org.jetbrains.kotlin.idea.decompiler.common.AnnotationLoaderForStubBuilderImpl
import org.jetbrains.kotlin.idea.decompiler.common.DirectoryBasedClassDataFinder
import org.jetbrains.kotlin.idea.decompiler.common.toClassProto
import org.jetbrains.kotlin.idea.decompiler.common.toPackageProto
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.ClsStubBuilderComponents
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.createPackageFacadeStub
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.createTopLevelClassStub
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import java.io.ByteArrayInputStream

public class KotlinBuiltInStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + 1

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {

        if (isInternalBuiltInFile(content.file)) return null

        return doBuildFileStub(content)
    }

    @TestOnly
    fun doBuildFileStub(content: FileContent): KotlinFileStubImpl? {
        val file = content.file
        val directory = file.parent!!
        val nameResolver = readStringTable(directory, LOG) ?: return null
        val contentAsBytes = content.content

        return when (file.fileType) {
            KotlinBuiltInPackageFileType -> {
                val packageProto = contentAsBytes.toPackageProto(BuiltInsSerializedResourcePaths.extensionRegistry)
                val packageFqName = packageProto.packageFqName(nameResolver)
                val components = createStubBuilderComponents(file, packageFqName, nameResolver)
                val context = components.createContext(
                        nameResolver, packageFqName, TypeTable(packageProto.typeTable)
                )
                createPackageFacadeStub(packageProto, packageFqName, context)
            }
            KotlinBuiltInClassFileType -> {
                val classProto = contentAsBytes.toClassProto(BuiltInsSerializedResourcePaths.extensionRegistry)
                val classId = nameResolver.getClassId(classProto.fqName)
                val packageFqName = classId.packageFqName
                val components = createStubBuilderComponents(file, packageFqName, nameResolver)
                val context = components.createContext(nameResolver, packageFqName, TypeTable(classProto.typeTable))
                createTopLevelClassStub(classId, classProto, context)
            }
            else -> error("Unexpected filetype ${file.fileType}")
        }
    }

    private fun createStubBuilderComponents(file: VirtualFile, packageFqName: FqName, nameResolver: NameResolver): ClsStubBuilderComponents {
        val finder = DirectoryBasedClassDataFinder(file.parent!!, packageFqName, nameResolver, BuiltInsSerializedResourcePaths)
        val annotationLoader = AnnotationLoaderForStubBuilderImpl(BuiltInSerializerProtocol)
        return ClsStubBuilderComponents(finder, annotationLoader)
    }

    companion object {
        val LOG = Logger.getInstance(KotlinBuiltInStubBuilder::class.java)
    }
}

fun readStringTable(directory: VirtualFile, log: Logger): NameResolverImpl? {
    val stringsFileName = "${directory.name}.${BuiltInsSerializedResourcePaths.STRING_TABLE_FILE_EXTENSION}"
    val stringTableFile = directory.findFileByRelativePath(stringsFileName) ?: return run {
        log.error("$stringsFileName not found in $directory")
        null
    }
    val nameResolver = try {
        NameResolverImpl.read(ByteArrayInputStream(stringTableFile.contentsToByteArray(false)))
    }
    catch (e: Exception) {
        log.error("Error reading data from $stringTableFile ", e)
        null
    }
    return nameResolver
}
