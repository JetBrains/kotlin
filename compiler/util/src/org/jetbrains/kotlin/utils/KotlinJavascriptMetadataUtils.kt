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

import java.io.File
import javax.xml.bind.DatatypeConverter.parseBase64Binary
import javax.xml.bind.DatatypeConverter.printBase64Binary

public class KotlinJavascriptMetadata(public val abiVersion: Int, public val moduleName: String, public val body: ByteArray) {
    public val TEMP_isAbiVersionCompatible: Boolean = KotlinJavascriptMetadataUtils.isAbiVersionCompatible(abiVersion)
}

public object KotlinJavascriptMetadataUtils {
    public val JS_EXT: String = ".js"
    public val META_JS_SUFFIX: String = ".meta.js"
    public val VFS_PROTOCOL: String = "kotlin-js-meta"
    private val KOTLIN_JAVASCRIPT_METHOD_NAME = "kotlin_module_metadata"
    private val KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN = "\\.kotlin_module_metadata\\(".toPattern()
    /**
     * Matches string like <name>.kotlin_module_metadata(<abi version>, <module name>, <base64 data>)
     */
    private val METADATA_PATTERN = "(?m)\\w+\\.$KOTLIN_JAVASCRIPT_METHOD_NAME\\((\\d+),\\s*(['\"])([^'\"]*)\\2,\\s*(['\"])([^'\"]*)\\4\\)".toPattern()

    @JvmStatic
    public val ABI_VERSION: Int = 3

    public fun replaceSuffix(filePath: String): String = filePath.substringBeforeLast(JS_EXT) + META_JS_SUFFIX

    @JvmStatic
    public fun isAbiVersionCompatible(abiVersion: Int): Boolean = abiVersion == ABI_VERSION

    @JvmStatic
    public fun hasMetadata(text: String): Boolean =
            KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN.matcher(text).find() && METADATA_PATTERN.matcher(text).find()

    public fun hasMetadataWithIncompatibleAbiVersion(text: String): Boolean {
        val matcher = METADATA_PATTERN.matcher(text)
        while (matcher.find()) {
            var abiVersion = matcher.group(1).toInt()
            if (abiVersion != ABI_VERSION) return true
        }
        return false
    }

    public fun formatMetadataAsString(moduleName: String, content: ByteArray): String =
        "// Kotlin.$KOTLIN_JAVASCRIPT_METHOD_NAME($ABI_VERSION, \"$moduleName\", \"${printBase64Binary(content)}\");\n"

    @JvmStatic
    public fun loadMetadata(file: File): List<KotlinJavascriptMetadata> {
        assert(file.exists()) { "Library " + file + " not found" }
        val metadataList = arrayListOf<KotlinJavascriptMetadata>()
        LibraryUtils.traverseJsLibrary(file) { content, relativePath ->
            var path = file.getPath()

            if (relativePath.isNotBlank()) {
                path += "/$relativePath"
            }

            parseMetadata(content, metadataList)
        }

        return metadataList
    }

    @JvmStatic
    public fun loadMetadata(path: String): List<KotlinJavascriptMetadata> = loadMetadata(File(path))

    @JvmStatic
    public fun parseMetadata(text: String, metadataList: MutableList<KotlinJavascriptMetadata>) {
        // Check for literal pattern first in order to reduce time for large files without metadata
        if (!KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN.matcher(text).find()) return

        val matcher = METADATA_PATTERN.matcher(text)
        while (matcher.find()) {
            var abiVersion = matcher.group(1).toInt()
            var moduleName = matcher.group(3)
            val data = matcher.group(5)
            metadataList.add(KotlinJavascriptMetadata(abiVersion, moduleName, parseBase64Binary(data)))
        }
    }
}
