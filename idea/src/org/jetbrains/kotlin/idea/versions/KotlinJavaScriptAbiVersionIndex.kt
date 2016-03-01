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

package org.jetbrains.kotlin.idea.versions

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.utils.JsBinaryVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.util.*

object KotlinJavaScriptAbiVersionIndex : KotlinAbiVersionIndexBase<KotlinJavaScriptAbiVersionIndex, JsBinaryVersion>(
        KotlinJavaScriptAbiVersionIndex::class.java, { JsBinaryVersion(*it) }
) {
    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter() { file -> JavaScript.EXTENSION == file.extension }

    override fun getVersion() = VERSION

    private val VERSION = 2

    private val INDEXER = DataIndexer { inputData: FileContent ->
        val result = HashMap<JsBinaryVersion, Void?>()

        tryBlock(inputData) {
            val text = VfsUtilCore.loadText(inputData.file)
            val metadataList = ArrayList<KotlinJavascriptMetadata>()
            KotlinJavascriptMetadataUtils.parseMetadata(text, metadataList)
            for (metadata in metadataList) {
                val version = if (KotlinJavascriptMetadataUtils.isAbiVersionCompatible(metadata.abiVersion)) {
                    JsBinaryVersion(0, metadata.abiVersion, 0)
                }
                else {
                    // Version is set to something weird
                    JsBinaryVersion.INVALID_VERSION
                }
                result[version] = null
            }
        }

        result
    }
}
