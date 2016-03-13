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

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.decompiler.js.JsMetaFileUtils
import org.jetbrains.kotlin.idea.decompiler.js.KotlinJavaScriptMetaFileType
import org.jetbrains.kotlin.name.FqName
import java.io.DataInput
import java.io.DataOutput
import java.util.*

abstract class KotlinFileIndexBase<T>(private val classOfIndex: Class<T>) : ScalarIndexExtension<FqName>() {
    val KEY: ID<FqName, Void> = ID.create(classOfIndex.canonicalName)

    private val KEY_DESCRIPTOR : KeyDescriptor<FqName> = object : KeyDescriptor<FqName> {
        override fun save(output: DataOutput, value: FqName) = output.writeUTF(value.asString())

        override fun read(input: DataInput) = FqName(input.readUTF())

        override fun getHashCode(value: FqName) = value.asString().hashCode()

        override fun isEqual(val1: FqName?, val2: FqName?) = val1 == val2
    }

    private val LOG = Logger.getInstance(classOfIndex)

    override fun getName() = KEY

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = KEY_DESCRIPTOR

    protected fun indexer(f: (FileContent) -> FqName?): DataIndexer<FqName, Void, FileContent> =
            // See KT-11323
            DataIndexer<FqName, Void, FileContent> {
                try {
                    val fqName = f(it)
                    if (fqName != null) {
                        Collections.singletonMap<FqName, Void>(fqName, null)
                    }
                    else {
                        emptyMap()
                    }
                }
                catch (e: Throwable) {
                    LOG.warn("Error while indexing file " + it.fileName, e)
                    emptyMap()
                }
            }
}

object KotlinClassFileIndex : KotlinFileIndexBase<KotlinClassFileIndex>(KotlinClassFileIndex::class.java) {

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter { file -> file.fileType == JavaClassFileType.INSTANCE }

    override fun getVersion() = VERSION

    private val VERSION = 2

    private val INDEXER = indexer() { fileContent ->
        val headerInfo = IDEKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(fileContent.file, fileContent.content)
        if (headerInfo != null && headerInfo.classHeader.metadataVersion.isCompatible()) headerInfo.classId.asSingleFqName() else null
    }
}

object KotlinJavaScriptMetaFileIndex : KotlinFileIndexBase<KotlinJavaScriptMetaFileIndex>(KotlinJavaScriptMetaFileIndex::class.java) {

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter { file -> file.fileType == KotlinJavaScriptMetaFileType }

    override fun getVersion() = VERSION

    private val VERSION = 2

    private val INDEXER = indexer() { fileContent ->
        if (fileContent.fileType == KotlinJavaScriptMetaFileType) JsMetaFileUtils.getClassFqName(fileContent.file) else null
    }

    override fun dependsOnFileContent(): Boolean = false
}
