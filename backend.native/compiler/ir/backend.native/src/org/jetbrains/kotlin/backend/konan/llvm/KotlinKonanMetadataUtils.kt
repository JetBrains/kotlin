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

 // this file has been adapted from the corresponding piece of Javascript compiler

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import java.io.File
import javax.xml.bind.DatatypeConverter.parseBase64Binary
import javax.xml.bind.DatatypeConverter.printBase64Binary


class KotlinKonanMetadata(val abiVersion: Int, val moduleName: String, val body: ByteArray) {
    val isAbiVersionCompatible: Boolean = KotlinKonanMetadataUtils.isAbiVersionCompatible(abiVersion)
}

class JsBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible() = this.isCompatibleTo(INSTANCE)

    companion object {
        val INSTANCE = JsBinaryVersion(0, 5, 0)

        val INVALID_VERSION = JsBinaryVersion()
    }
}

object KotlinKonanMetadataUtils {
    private val KOTLIN_JAVASCRIPT_METHOD_NAME = "kotlin_module_metadata"
    private val KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN = "\\.kotlin_module_metadata\\(".toPattern()

    /**
     * Matches string like <name>.kotlin_module_metadata(<abi version>, <module name>, <base64 data>)
     */
    private val METADATA_PATTERN = "(?m)\\w+\\.$KOTLIN_JAVASCRIPT_METHOD_NAME\\((\\d+),\\s*(['\"])([^'\"]*)\\2,\\s*(['\"])([^'\"]*)\\4\\)".toPattern()

    val ABI_VERSION: Int = JsBinaryVersion.INSTANCE.minor

    fun isAbiVersionCompatible(abiVersion: Int): Boolean = abiVersion == ABI_VERSION

    fun hasMetadata(text: String): Boolean =
            KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN.matcher(text).find() && METADATA_PATTERN.matcher(text).find()

    fun formatMetadataAsString(moduleName: String, content: ByteArray): String =
        "// Kotlin.$KOTLIN_JAVASCRIPT_METHOD_NAME($ABI_VERSION, \"$moduleName\", \"${printBase64Binary(content)}\");\n"

    fun loadMetadata(file: File): List<KotlinKonanMetadata> {
        assert(file.exists()) { "Library $file not found" }
        val metadataList = arrayListOf<KotlinKonanMetadata>()
        val content = readModuleMetadata(file)
        parseMetadata(content, metadataList)

        return metadataList
    }

    fun loadMetadata(path: String): List<KotlinKonanMetadata> = loadMetadata(File(path))

    fun parseMetadata(text: String, metadataList: MutableList<KotlinKonanMetadata>) {
        // Check for literal pattern first in order to reduce time for large files without metadata
        if (!KOTLIN_JAVASCRIPT_METHOD_NAME_PATTERN.matcher(text).find()) return

        val matcher = METADATA_PATTERN.matcher(text)
        while (matcher.find()) {
            val abiVersion = matcher.group(1).toInt()
            val moduleName = matcher.group(3)
            val data = matcher.group(5)
            metadataList.add(KotlinKonanMetadata(abiVersion, moduleName, parseBase64Binary(data)))
        }
    }

    fun loadLibMetadata(libraries: List<String>): List<KotlinKonanMetadata> {

        val allMetadata = mutableListOf<KotlinKonanMetadata>()

        for (path in libraries) {
            val filePath = File(path)
            if (!filePath.exists()) {
                // TODO: should we throw here?
                println("Path '" + path + "' does not exist");
            }

            val metadataList = loadMetadata(filePath);

            if (metadataList.isEmpty()) {
                // TODO: should we throw here?
                println("'" + path + "' is not a valid Kotlin Native library");
            }

            for (metadata in metadataList) {
                if (!metadata.isAbiVersionCompatible) {
                    // TODO: should we throw here?
                    println("File '" + path + "' was compiled with an incompatible version of Kotlin. " +
                            "Its ABI version is " + metadata.abiVersion +
                            ", expected version is " + ABI_VERSION);
                }
            }

            allMetadata.addAll(metadataList);
        }
        return allMetadata;
    }
}

