/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.js

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.decompiler.common.FileWithMetadata
import org.jetbrains.kotlin.idea.decompiler.common.KotlinMetadataDecompiler
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.utils.JsMetadataVersion
import java.io.ByteArrayInputStream

class KotlinJavaScriptMetaFileDecompiler : KotlinMetadataDecompiler<JsMetadataVersion>(
        KotlinJavaScriptMetaFileType, JsPlatform, JsSerializerProtocol, DynamicTypeDeserializer,
        JsMetadataVersion.INSTANCE, JsMetadataVersion.INVALID_VERSION, KotlinStubVersions.JS_STUB_VERSION
) {
    override fun readFile(bytes: ByteArray, file: VirtualFile): FileWithMetadata? {
        val stream = ByteArrayInputStream(bytes)

        val version = JsMetadataVersion.readFrom(stream)
        if (!version.isCompatible()) {
            return FileWithMetadata.Incompatible(version)
        }

        JsProtoBuf.Header.parseDelimitedFrom(stream)

        val proto = ProtoBuf.PackageFragment.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
        return FileWithMetadata.Compatible(proto, JsSerializerProtocol)
    }
}
