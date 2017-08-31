/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.reference.SoftReference
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId

object IDEKotlinBinaryClassCache {
    data class KotlinBinaryHeaderData(val classHeader: KotlinClassHeader, val classId: ClassId)
    data class KotlinBinaryData(val isKotlinBinary: Boolean, val timestamp: Long, val headerData: KotlinBinaryHeaderData?)

    /**
     * Checks if this file is a compiled Kotlin class file (not necessarily ABI-compatible with the current plugin)
     */
    fun isKotlinJvmCompiledFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        if (file.extension != JavaClassFileType.INSTANCE!!.defaultExtension) {
            return false
        }

        val cached = getKotlinBinaryFromCache(file)
        if (cached != null) {
            return cached.isKotlinBinary
        }
        return getKotlinBinaryClass(file, fileContent) != null
    }

    fun getKotlinBinaryClass(file: VirtualFile, fileContent: ByteArray? = null): KotlinJvmBinaryClass? {
        val cached = getKotlinBinaryFromCache(file)
        if (cached != null && !cached.isKotlinBinary) {
            return null
        }

        val kotlinBinaryClass = KotlinBinaryClassCache.getKotlinBinaryClass(file, fileContent)

        val isKotlinBinaryClass = kotlinBinaryClass != null
        if (file is VirtualFileWithId) {
            attributeService.writeBooleanAttribute(KOTLIN_IS_COMPILED_FILE_ATTRIBUTE, file, isKotlinBinaryClass)
        }

        if (isKotlinBinaryClass) {
            val headerInfo = createHeaderInfo(kotlinBinaryClass)
            file.putUserData(KOTLIN_BINARY_DATA_KEY, SoftReference(KotlinBinaryData(isKotlinBinaryClass, file.timeStamp, headerInfo)))
        }

        return kotlinBinaryClass
    }

    fun getKotlinBinaryClassHeaderData(file: VirtualFile, fileContent: ByteArray? = null): KotlinBinaryHeaderData? {
        val cached = getKotlinBinaryFromCache(file)
        if (cached != null) {
            if (!cached.isKotlinBinary) {
                return null
            }
            if (cached.headerData != null) {
                return cached.headerData
            }
        }

        val kotlinBinaryClass = getKotlinBinaryClass(file, fileContent)
        return createHeaderInfo(kotlinBinaryClass)
    }

    private val attributeService = ServiceManager.getService(FileAttributeService::class.java)

    private fun createHeaderInfo(kotlinBinaryClass: KotlinJvmBinaryClass?): KotlinBinaryHeaderData? {
        val header = kotlinBinaryClass?.classHeader
        val classId = kotlinBinaryClass?.classId

        return if (header != null && classId != null) KotlinBinaryHeaderData(header, classId) else null
    }

    private val KOTLIN_IS_COMPILED_FILE_ATTRIBUTE: String = "kotlin-is-binary-compiled".apply {
        ServiceManager.getService(FileAttributeService::class.java)?.register(this, 1)
    }

    private val KOTLIN_BINARY_DATA_KEY = Key.create<SoftReference<KotlinBinaryData>>(KOTLIN_IS_COMPILED_FILE_ATTRIBUTE)

    private fun getKotlinBinaryFromCache(file: VirtualFile): KotlinBinaryData? {
        val userData = file.getUserData(KOTLIN_BINARY_DATA_KEY)?.get()
        if (userData != null && userData.timestamp == file.timeStamp) {
            return userData
        }

        val isKotlinBinaryAttribute = if (file is VirtualFileWithId)
            attributeService.readBooleanAttribute(KOTLIN_IS_COMPILED_FILE_ATTRIBUTE, file)
        else
            null

        if (isKotlinBinaryAttribute != null) {
            val isKotlinBinary = isKotlinBinaryAttribute.value
            val kotlinBinaryData = KotlinBinaryData(isKotlinBinary, file.timeStamp, null)
            if (isKotlinBinary) {
                file.putUserData(KOTLIN_BINARY_DATA_KEY, SoftReference(kotlinBinaryData))
            }

            return kotlinBinaryData
        }

        return null
    }
}