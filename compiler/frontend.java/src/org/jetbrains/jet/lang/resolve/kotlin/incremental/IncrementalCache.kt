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
import com.intellij.util.io.IntInlineKeyDescriptor
import org.jetbrains.org.objectweb.asm.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import java.util.TreeMap

public class IncrementalCache(baseDir: File) {
    class object {
        val PROTO_MAP = "proto.tab"
        val CONSTANTS_MAP = "constants.tab"

        private fun getConstantsHash(bytes: ByteArray): Int {
            val result = TreeMap<String, Any>() // keys order should defined to check hash of a map

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                    if (value != null) {
                        result[name] = value
                    }
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            return result.hashCode()
        }
    }

    private val protoData: PersistentMap<ClassOrPackageId, ByteArray>
    private val constantsData = PersistentHashMap(File(baseDir, CONSTANTS_MAP), EnumeratorStringDescriptor(), IntInlineKeyDescriptor())

    ;{
        protoData = PersistentHashMap(File(baseDir, PROTO_MAP), object : KeyDescriptor<ClassOrPackageId> {
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
        val fileBytes = file.readBytes()
        val classNameAndHeader = VirtualFileKotlinClass.readClassNameAndHeader(fileBytes)
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
        if (header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART) {
            return putConstantsData(className, getConstantsHash(fileBytes))
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

    private fun putConstantsData(packagePartClass: JvmClassName, constantsHash: Int): Boolean {
        val key = packagePartClass.getInternalName()

        val oldHash = constantsData[key]
        if (oldHash == constantsHash) {
            return false
        }
        constantsData.put(key, constantsHash)
        return true
    }

    public fun getPackageData(moduleId: String, fqName: FqName): ByteArray? {
        return protoData[ClassOrPackageId(moduleId, fqName)]
    }

    public fun close() {
        protoData.close()
        constantsData.close()
    }

    private data class ClassOrPackageId(val moduleId: String, val fqName: FqName) {
    }
}
