/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin.incremental

import java.io.File
import com.intellij.util.io.PersistentMap
import com.intellij.util.io.PersistentHashMap
import com.intellij.util.io.KeyDescriptor
import java.io.DataOutput
import com.intellij.util.io.IOUtil
import java.io.DataInput
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.util.io.DataExternalizer
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.descriptors.serialization.BitEncoding
import org.jetbrains.jet.utils.intellij.*
import java.util.Arrays

public class IncrementalCache(baseDir: File) {
    class object {
        val PROTO_MAP: String = "proto.tab"
    }

    private val protoData: PersistentMap<ClassOrPackageId, ByteArray>

    {
        protoData = PersistentHashMap<ClassOrPackageId, ByteArray>(File(baseDir, PROTO_MAP), object : KeyDescriptor<ClassOrPackageId> {
            override fun save(out: DataOutput, value: ClassOrPackageId?) {
                IOUtil.writeString(value!!.moduleId, out)
                IOUtil.writeString(value.fqName.asString(), out)
            }

            override fun read(`in`: DataInput): ClassOrPackageId {
                val module = IOUtil.readString(`in`)!!
                val fqName = FqName(IOUtil.readString(`in`)!!)
                return ClassOrPackageId(module, fqName)
            }

            override fun getHashCode(value: ClassOrPackageId?): Int {
                return value?.hashCode() ?: -1
            }

            override fun isEqual(val1: ClassOrPackageId?, val2: ClassOrPackageId?): Boolean {
                return val1 == val2
            }
        }, object : DataExternalizer<ByteArray> {
            override fun save(out: DataOutput, value: ByteArray?) {
                out.writeInt(value!!.size)
                out.write(value)
            }

            override fun read(`in`: DataInput): ByteArray {
                val length = `in`.readInt()
                val buf = ByteArray(length)
                `in`.readFully(buf)
                return buf
            }
        })
    }

    public fun saveFileToCache(moduleId: String, file: File): Boolean {
        val classNameAndHeader = VirtualFileKotlinClass.readClassNameAndHeader(file.readBytes())
        if (classNameAndHeader == null) return false

        val (className, header) = classNameAndHeader
        val classFqName = className.getFqNameForClassNameWithoutDollars()
        val annotationDataEncoded = header.annotationData
        if (annotationDataEncoded != null) {
            val data = BitEncoding.decodeBytes(annotationDataEncoded)
            when (header.kind) {
                KotlinClassHeader.Kind.PACKAGE_FACADE -> {
                    return putData(moduleId, classFqName.parent(), data)
                }
                KotlinClassHeader.Kind.CLASS -> {
                    return putData(moduleId, classFqName, data)
                }
            }
        }

        return false
    }

    private fun putData(moduleId: String, fqName: FqName, data: ByteArray): Boolean {
        val id = ClassOrPackageId(moduleId, fqName)
        val oldData = protoData[id]
        if (Arrays.equals(data, oldData)) {
            return false
        }
        protoData.put(id, data)
        return true
    }

    public fun getPackageData(moduleId: String, fqName: FqName): ByteArray? {
        return protoData[ClassOrPackageId(moduleId, fqName)]
    }

    public fun close() {
        protoData.close()
    }

    private data class ClassOrPackageId(val moduleId: String, val fqName: FqName) {
    }
}
