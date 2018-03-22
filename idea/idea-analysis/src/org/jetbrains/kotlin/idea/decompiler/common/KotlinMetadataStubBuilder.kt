/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.common

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.*
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId

open class KotlinMetadataStubBuilder(
        private val version: Int,
        private val fileType: FileType,
        private val serializerProtocol: SerializerExtensionProtocol,
        private val readFile: (ByteArray, VirtualFile) -> FileWithMetadata?
) : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + version

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        assert(virtualFile.fileType == fileType) { "Unexpected file type ${virtualFile.fileType}" }
        val file = readFile(content.content, virtualFile) ?: return null

        when (file) {
            is FileWithMetadata.Incompatible -> {
                return createIncompatibleAbiVersionFileStub()
            }
            is FileWithMetadata.Compatible -> {
                val packageProto = file.proto.`package`
                val packageFqName = file.packageFqName
                val nameResolver = file.nameResolver
                val components = ClsStubBuilderComponents(
                        ProtoBasedClassDataFinder(file.proto, nameResolver),
                        AnnotationLoaderForStubBuilderImpl(serializerProtocol),
                        virtualFile
                )
                val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))

                val fileStub = createFileStub(packageFqName, isScript = false)
                createDeclarationsStubs(
                        fileStub, context,
                        ProtoContainer.Package(packageFqName, context.nameResolver, context.typeTable, source = null),
                        packageProto
                )
                for (classProto in file.classesToDecompile) {
                    createClassStub(
                            fileStub, classProto, nameResolver, nameResolver.getClassId(classProto.fqName), source = null, context = context
                    )
                }
                return fileStub
            }
        }
    }
}
