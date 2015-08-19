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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.idea.decompiler.KotlinJavaScriptMetaFileType
import org.jetbrains.kotlin.idea.decompiler.navigation.JsMetaFileUtils
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.name.FqName
import java.io.DataInput
import java.io.DataOutput
import java.util.Collections

abstract class KotlinFileIndexBase<T>(private val classOfIndex: Class<T>) : ScalarIndexExtension<FqName>() {
    public val KEY: ID<FqName, Void> = ID.create(classOfIndex.getCanonicalName())

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

    protected fun indexer(f: (VirtualFile) -> FqName?): DataIndexer<FqName, Void, FileContent> =
            DataIndexer<FqName, Void, FileContent> {
                try {
                    val fqName = f(it.getFile())
                    if (fqName != null) {
                        Collections.singletonMap<FqName, Void>(fqName, null)
                    }
                    else {
                        emptyMap()
                    }
                }
                catch (e: Throwable) {
                    LOG.warn("Error while indexing file " + it.getFileName(), e)
                    emptyMap()
                }
            }
}

public object KotlinClassFileIndex : KotlinFileIndexBase<KotlinClassFileIndex>(javaClass<KotlinClassFileIndex>()) {

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter { file -> file.getFileType() == JavaClassFileType.INSTANCE }

    override fun getVersion() = VERSION

    private val VERSION = 2

    private val INDEXER = indexer() { file ->
        val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(file)
        if (kotlinClass != null && kotlinClass.getClassHeader().isCompatibleAbiVersion) kotlinClass.getClassId().asSingleFqName() else null
    }
}

public object KotlinJavaScriptMetaFileIndex : KotlinFileIndexBase<KotlinJavaScriptMetaFileIndex>(javaClass<KotlinJavaScriptMetaFileIndex>()) {

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter { file -> file.getFileType() == KotlinJavaScriptMetaFileType }

    override fun getVersion() = VERSION

    private val VERSION = 1

    private val INDEXER = indexer() { file ->
        if (file.getFileType() == KotlinJavaScriptMetaFileType) JsMetaFileUtils.getClassFqName(file) else null
    }
}
