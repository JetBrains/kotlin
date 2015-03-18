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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.IOException
import javax.xml.bind.DatatypeConverter.parseBase64Binary
import javax.xml.bind.DatatypeConverter.printBase64Binary
import kotlin.platform.platformStatic

public class KotlinJavascriptMetadata(public val moduleName: String, public val body: ByteArray)

public object KotlinJavascriptMetadataUtils {
    private val LOG = Logger.getInstance(javaClass<KotlinJavascriptMetadataUtils>())

    public val JS_EXT: String = ".js"
    private val KOTLIN_JAVASCRIPT_METHOD_NAME = "kotlin_module_metadata"
    /**
     * Matches string like <name>.kotlin_module_metadata(<abi version>, <module name>, <base64 data>)
     */
    private val METADATA_PATTERN = "(?m)\\w+\\.$KOTLIN_JAVASCRIPT_METHOD_NAME\\((\\d+),\\s*(['\"])([^'\"]*)\\2,\\s*(['\"])([^'\"]*)\\4\\)".toRegex()
    private val ABI_VERSION = 1

    platformStatic
    public fun hasMetadata(text: String): Boolean = METADATA_PATTERN.matcher(text).find()

    public fun writeMetadata(moduleName: String, content: ByteArray, metaFile: File) {
        val text = "// Kotlin.$KOTLIN_JAVASCRIPT_METHOD_NAME($ABI_VERSION, \"$moduleName\", \"${printBase64Binary(content)}\");\n"
        FileUtil.writeToFile(metaFile, text)
    }

    platformStatic
    public fun loadMetadata(file: File): List<KotlinJavascriptMetadata> {
        assert(file.exists()) { "Library " + file + " not found" }

        var result: MutableList<KotlinJavascriptMetadata> = arrayListOf()
        if (file.isDirectory()) {
            loadMetadataFromDirectory(file, result)
        }
        else if (file.getName().endsWith(JS_EXT)) {
            loadMetadataFromFile(file, result)
        }
        else {
            loadMetadataFromZip(file, result)
        }
        return result
    }

    platformStatic
    public fun loadMetadata(path: String): List<KotlinJavascriptMetadata> = loadMetadata(File(path))

    private fun parseMetadata(text: String, path: String, metadataList: MutableList<KotlinJavascriptMetadata>) {
        val matcher = METADATA_PATTERN.matcher(text)
        while (matcher.find()) {
            var abiVersion = Integer.parseInt(matcher.group(1));
            if (abiVersion != ABI_VERSION) LOG.error("Unsupported abi version in $path, expected $ABI_VERSION, but $abiVersion")

            var moduleName = matcher.group(3)
            val data = matcher.group(5)
            metadataList.add(KotlinJavascriptMetadata(moduleName, parseBase64Binary(data)))
        }
    }

    private fun loadMetadataFromFile(file: File, metadataList: MutableList<KotlinJavascriptMetadata>) {
        try {
            val content = FileUtil.loadFile(file)
            parseMetadata(content, file.getPath(), metadataList)
        }
        catch (ex: IOException) {
            LOG.error("Could not read ${file.getAbsolutePath()}: ${ex.getMessage()}")
        }
    }

    private fun loadMetadataFromDirectory(dir: File, metadataList: MutableList<KotlinJavascriptMetadata>) {
        LibraryUtils.traverseDirectory(dir) {
            file, relativePath -> parseMetadata(FileUtil.loadFile(file), file.getPath(), metadataList)
        }
    }

    private fun loadMetadataFromZip(file: File, metadataList: MutableList<KotlinJavascriptMetadata>) {
        LibraryUtils.traverseArchive(file) { content, relativePath ->
            parseMetadata(content, "${file.getPath()}/$relativePath", metadataList)
        }
    }
}
