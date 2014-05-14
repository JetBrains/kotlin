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

public class IncrementalCache(baseDir: File) {
    class object {
        val PACKAGE_MAP: String = "package.tab"
    }

    private val packageFacadeData: PersistentMap<PackageFacadeId, ByteArray>

    {
        packageFacadeData = PersistentHashMap<PackageFacadeId, ByteArray>(File(baseDir, PACKAGE_MAP), object : KeyDescriptor<PackageFacadeId> {
            override fun save(out: DataOutput, value: PackageFacadeId?) {
                IOUtil.writeString(value!!.moduleId, out)
                IOUtil.writeString(value.fqName.asString(), out)
            }

            override fun read(`in`: DataInput): PackageFacadeId {
                val module = IOUtil.readString(`in`)!!
                val fqName = FqName(IOUtil.readString(`in`)!!)
                return PackageFacadeId(module, fqName)
            }

            override fun getHashCode(value: PackageFacadeId?): Int {
                return value?.hashCode() ?: -1
            }

            override fun isEqual(val1: PackageFacadeId?, val2: PackageFacadeId?): Boolean {
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

    public fun saveFileToCache(moduleId: String, file: File) {
        val classNameAndHeader = VirtualFileKotlinClass.readClassNameAndHeader(file.readBytes())
        if (classNameAndHeader == null) return

        val (className, header) = classNameAndHeader
        if (header.kind == KotlinClassHeader.Kind.PACKAGE_FACADE) {
            putPackageData(moduleId, className.getFqNameForClassNameWithoutDollars().parent(), BitEncoding.decodeBytes(header.annotationData!!))
        }
    }

    public fun putPackageData(moduleId: String, fqName: FqName, data: ByteArray) {
        packageFacadeData.put(PackageFacadeId(moduleId, fqName), data)
    }

    public fun getPackageData(moduleId: String, fqName: FqName): ByteArray? {
        return packageFacadeData[PackageFacadeId(moduleId, fqName)]
    }

    public fun close() {
        packageFacadeData.close()
    }

    private data class PackageFacadeId(val moduleId: String, val fqName: FqName) {
    }
}
