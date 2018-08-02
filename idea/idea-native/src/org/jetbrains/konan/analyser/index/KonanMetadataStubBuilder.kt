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

package org.jetbrains.konan.analyser.index

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.konan.KotlinWorkaroundUtil.createFileStub
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.serialization.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.createIncompatibleAbiVersionFileStub
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

//todo: Fix in Kotlin plugin
open class KonanMetadataStubBuilder(
  private val version: Int,
  private val fileType: FileType,
  private val serializerProtocol: SerializerExtensionProtocol,
  private val readFile: (VirtualFile) -> FileWithMetadata?
) : ClsStubBuilder() {

  override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + version

  override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
    val virtualFile = content.file
    assert(virtualFile.fileType == fileType) { "Unexpected file type ${virtualFile.fileType}" }

    val file = readFile(virtualFile) ?: return null

    return when (file) {
      is FileWithMetadata.Incompatible -> createIncompatibleAbiVersionFileStub()
      is FileWithMetadata.Compatible -> { //todo: this part is implemented in our own way
        val renderer = DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
        val ktFileText = decompiledText(file, KonanPlatform, serializerProtocol, NullFlexibleTypeDeserializer, renderer)
        createFileStub(content.project, ktFileText.text)
      }
    }
  }
}
