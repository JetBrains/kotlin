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

package org.jetbrains.kotlin.load.kotlin

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile

class KotlinBinaryClassCache : Disposable {
    private class RequestCache {
        internal var virtualFile: VirtualFile? = null
        internal var modificationStamp: Long = 0
        internal var virtualFileKotlinClass: VirtualFileKotlinClass? = null

        fun cache(file: VirtualFile, aClass: VirtualFileKotlinClass?): VirtualFileKotlinClass? {
            virtualFile = file
            virtualFileKotlinClass = aClass
            modificationStamp = file.modificationStamp

            return aClass
        }
    }

    private val cache = object : ThreadLocal<RequestCache>() {
        override fun initialValue(): RequestCache {
            return RequestCache()
        }
    }

    override fun dispose() {
        // This is only relevant for tests. We create a new instance of Application for each test, and so a new instance of this service is
        // also created for each test. However all tests share the same event dispatch thread, which would collect all instances of this
        // thread-local if they're not removed properly. Each instance would transitively retain VFS resulting in OutOfMemoryError
        cache.remove()
    }

    companion object {
        fun getKotlinBinaryClass(file: VirtualFile, fileContent: ByteArray? = null): KotlinJvmBinaryClass? {
            if (file.fileType !== JavaClassFileType.INSTANCE) return null

            val service = ServiceManager.getService(KotlinBinaryClassCache::class.java)
            val requestCache = service.cache.get()

            if (file.modificationStamp == requestCache.modificationStamp && file == requestCache.virtualFile) {
                return requestCache.virtualFileKotlinClass
            }

            val aClass = ApplicationManager.getApplication().runReadAction(Computable {
                @Suppress("DEPRECATION")
                VirtualFileKotlinClass.create(file, fileContent)
            })

            return requestCache.cache(file, aClass)
        }
    }
}
