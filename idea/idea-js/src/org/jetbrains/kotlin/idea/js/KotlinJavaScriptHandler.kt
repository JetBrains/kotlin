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

package org.jetbrains.kotlin.idea.js

import com.intellij.openapi.vfs.impl.ArchiveHandler
import com.intellij.openapi.vfs.impl.ArchiveHandler.EntryInfo
import com.intellij.util.ArrayUtil
import gnu.trove.THashMap
import org.jetbrains.kotlin.serialization.js.forEachFile
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils

public open class KotlinJavaScriptHandler(path: String) : ArchiveHandler(path) {

    override fun createEntriesMap(): Map<String, EntryInfo> {
        val map = THashMap<String, EntryInfo>()
        map.put("", createRootEntry())

        for(metadata in KotlinJavascriptMetadataUtils.loadMetadata(getFile())) {
            val moduleName = metadata.moduleName
            metadata.forEachFile {
                filePath, fileContent -> getOrCreate(moduleName + "/" + filePath, false, map, fileContent)
            }
        }

        return map
    }

    private fun getOrCreate(
            entryPath: String,
            isDirectory: Boolean,
            map: MutableMap<String, EntryInfo>,
            content: ByteArray = ArrayUtil.EMPTY_BYTE_ARRAY
    ): EntryInfo {
        val info = map[entryPath]
        if (info != null) return info

        val path = splitPath(entryPath)
        val parentInfo = getOrCreate(path.first, true, map)
        val newInfo = JsMetaEntryInfo(parentInfo, path.second, isDirectory, content)
        map[entryPath] = newInfo

        return newInfo
    }

    override fun contentsToByteArray(relativePath: String): ByteArray {
        val entryInfo = getEntryInfo(relativePath)
        return if (entryInfo is JsMetaEntryInfo) entryInfo.content else ArrayUtil.EMPTY_BYTE_ARRAY
    }

    private class JsMetaEntryInfo(
            parent: EntryInfo,
            shortName: String,
            isDirectory: Boolean,
            val content: ByteArray
    ) : EntryInfo(shortName, isDirectory, ArchiveHandler.DEFAULT_LENGTH, ArchiveHandler.DEFAULT_TIMESTAMP, parent)
}