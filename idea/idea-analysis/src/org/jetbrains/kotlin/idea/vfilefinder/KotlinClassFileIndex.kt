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
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.FqName

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.Collections

public class KotlinClassFileIndex : ScalarIndexExtension<FqName>() {

    override fun getName(): ID<FqName, Void> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<FqName, Void, FileContent> {
        return INDEXER
    }

    override fun getKeyDescriptor(): KeyDescriptor<FqName> {
        return KEY_DESCRIPTOR
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return INPUT_FILTER
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }

    override fun getVersion(): Int {
        return VERSION
    }

    companion object {

        private val LOG = Logger.getInstance(javaClass<KotlinClassFileIndex>())
        private val VERSION = 2
        public val KEY: ID<FqName, Void> = ID.create<FqName, Void>(javaClass<KotlinClassFileIndex>().getCanonicalName())

        private val KEY_DESCRIPTOR = object : KeyDescriptor<FqName> {
            throws(IOException::class)
            override fun save(out: DataOutput, value: FqName) {
                out.writeUTF(value.asString())
            }

            throws(IOException::class)
            override fun read(`in`: DataInput): FqName {
                return FqName(`in`.readUTF())
            }

            override fun getHashCode(value: FqName): Int {
                return value.asString().hashCode()
            }

            override fun isEqual(val1: FqName?, val2: FqName?): Boolean {
                return if (val1 == null) val2 == null else val1 == val2
            }
        }

        private val INPUT_FILTER = object : FileBasedIndex.InputFilter {
            override fun acceptInput(file: VirtualFile): Boolean {
                return file.getFileType() == JavaClassFileType.INSTANCE
            }
        }
        public val INDEXER: DataIndexer<FqName, Void, FileContent> = object : DataIndexer<FqName, Void, FileContent> {
            override fun map(inputData: FileContent): Map<FqName, Void> {
                try {
                    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(inputData.getFile())
                    if (kotlinClass != null && kotlinClass.getClassHeader().isCompatibleAbiVersion) {
                        return Collections.singletonMap<FqName, Void>(kotlinClass.getClassId().asSingleFqName(), null)
                    }
                }
                catch (e: Throwable) {
                    LOG.warn("Error while indexing file " + inputData.getFileName(), e)
                }

                return emptyMap()
            }
        }
    }
}
