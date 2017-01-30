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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import java.io.DataInput
import java.io.DataOutput

/**
 * Important! This is not a stub-based index. And it has its own version
 */
abstract class KotlinMetadataVersionIndexBase<T, V : BinaryVersion>(
        private val classOfIndex: Class<T>,
        protected val createBinaryVersion: (IntArray) -> V
) : ScalarIndexExtension<V>() {

    override fun getName(): ID<V, Void> = ID.create<V, Void>(classOfIndex.canonicalName)

    override fun getKeyDescriptor(): KeyDescriptor<V> = object : KeyDescriptor<V> {
        override fun isEqual(val1: V, val2: V): Boolean = val1 == val2

        override fun getHashCode(value: V): Int = value.hashCode()

        override fun read(input: DataInput): V {
            val size = DataInputOutputUtil.readINT(input)
            return createBinaryVersion((0..size - 1).map { DataInputOutputUtil.readINT(input) }.toIntArray())
        }

        override fun save(output: DataOutput, value: V) {
            val array = value.toArray()
            DataInputOutputUtil.writeINT(output, array.size)
            for (number in array) {
                DataInputOutputUtil.writeINT(output, number)
            }
        }
    }

    override fun dependsOnFileContent() = true

    protected val LOG: Logger = Logger.getInstance(classOfIndex)

    protected inline fun tryBlock(inputData: FileContent, body: () -> Unit) {
        try {
            body()
        }
        catch (e: Throwable) {
            LOG.warn("Could not index ABI version for file " + inputData.file + ": " + e.message)
        }
    }
}
