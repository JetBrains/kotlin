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

package org.jetbrains.kotlin.utils

import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import javax.xml.bind.DatatypeConverter.parseBase64Binary
import javax.xml.bind.DatatypeConverter.printBase64Binary

class KotlinJavascriptMetadata(val abiVersion: Int, val moduleName: String, val body: ByteArray) {
    val isAbiVersionCompatible: Boolean = KotlinJavascriptMetadataUtils.isAbiVersionCompatible(abiVersion)
}

// TODO: move to JS modules
class JsBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible() = this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = JsBinaryVersion(0, 8, 0)

        @JvmField
        val INVALID_VERSION = JsBinaryVersion()

        fun readFrom(stream: InputStream): JsBinaryVersion {
            val dataInput = DataInputStream(stream)
            val size = dataInput.readInt()

            // We assume here that the version will always have 3 components. This is needed to prevent reading an unpredictable amount
            // of integers from old .kjsm files (pre-1.1) because they did not have the version in the beginning
            if (size != INSTANCE.toArray().size) return INVALID_VERSION

            return JsBinaryVersion(*(1..size).map { dataInput.readInt() }.toIntArray())
        }
    }
}

object KotlinJavascriptMetadataUtils {
    const val JS_EXT: String = ".js"
    const val META_JS_SUFFIX: String = ".meta.js"
    const val VFS_PROTOCOL: String = "kotlin-js-meta"
    private val KOTLIN_JAVASCRIPT_METHOD_NAME = "kotlin_module_metadata"
    private val KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN = "\\.kotlin_module_metadata\\(".toPattern()

    /**
     * Matches string like <name>.kotlin_module_metadata(<abi version>, <module name>, <base64 data>)
     */
    private val METADATA_PATTERN = "(?m)\\w+\\.$KOTLIN_JAVASCRIPT_METHOD_NAME\\((\\d+),\\s*(['\"])([^'\"]*)\\2,\\s*(['\"])([^'\"]*)\\4\\)".toPattern()

    @JvmField val ABI_VERSION: Int = JsBinaryVersion.INSTANCE.minor

    fun replaceSuffix(filePath: String): String = filePath.substringBeforeLast(JS_EXT) + META_JS_SUFFIX

    @JvmStatic fun isAbiVersionCompatible(abiVersion: Int): Boolean = abiVersion == ABI_VERSION

    @JvmStatic fun hasMetadata(text: String): Boolean =
            KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN.matcher(text).find() && METADATA_PATTERN.matcher(text).find()

    fun formatMetadataAsString(moduleName: String, content: ByteArray): String =
        "// Kotlin.$KOTLIN_JAVASCRIPT_METHOD_NAME($ABI_VERSION, \"$moduleName\", \"${printBase64Binary(content)}\");\n"

    @JvmStatic fun loadMetadata(file: File): List<KotlinJavascriptMetadata> {
        assert(file.exists()) { "Library $file not found" }
        val metadataList = arrayListOf<KotlinJavascriptMetadata>()
        JsLibraryUtils.traverseJsLibrary(file) { content, relativePath ->
            parseMetadata(content, metadataList)
        }

        return metadataList
    }

    @JvmStatic fun loadMetadata(path: String): List<KotlinJavascriptMetadata> = loadMetadata(File(path))

    @JvmStatic fun parseMetadata(text: String, metadataList: MutableList<KotlinJavascriptMetadata>) {
        // Check for literal pattern first in order to reduce time for large files without metadata
        if (!KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN.matcher(text).find()) return

        val matcher = METADATA_PATTERN.matcher(text)
        while (matcher.find()) {
            val abiVersion = matcher.group(1).toInt()
            val moduleName = matcher.group(3)
            val data = matcher.group(5)
            metadataList.add(KotlinJavascriptMetadata(abiVersion, moduleName, parseBase64Binary(data)))
        }
    }
}
