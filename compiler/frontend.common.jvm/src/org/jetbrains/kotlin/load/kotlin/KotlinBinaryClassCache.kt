/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.utils.ReusableByteArray
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList


class KotlinBinaryClassCache : Disposable {
    private val requestCaches = CopyOnWriteArrayList<WeakReference<RequestCache>>()

    private class RequestCache {
        var virtualFile: VirtualFile? = null
        var modificationStamp: Long = 0
        var result: KotlinClassFinder.Result? = null

        // this makes cached ReusableByteArray instances to be clearly visible in debugger
        private val cachedRefDelta = 1000

        fun cache(
            file: VirtualFile,
            result: KotlinClassFinder.Result?
        ): KotlinClassFinder.Result? {
            check(virtualFile == null)
            result?.contentRef?.addRef(cachedRefDelta)
            virtualFile = file
            modificationStamp = file.modificationStamp
            this.result = result
            return result
        }

        fun clear() {
            if (virtualFile == null) return
            virtualFile = null
            modificationStamp = 0
            result?.contentRef?.release(cachedRefDelta)
            result = null
        }
    }

    private val cache = object : ThreadLocal<RequestCache>() {
        override fun initialValue(): RequestCache {
            return RequestCache().also {
                requestCaches.add(WeakReference(it))
            }
        }
    }

    override fun dispose() {
        for (cache in requestCaches) {
            cache.get()?.clear()
        }
        requestCaches.clear()
        // This is only relevant for tests. We create a new instance of Application for each test, and so a new instance of this service is
        // also created for each test. However all tests share the same event dispatch thread, which would collect all instances of this
        // thread-local if they're not removed properly. Each instance would transitively retain VFS resulting in OutOfMemoryError
        cache.remove()
    }

    companion object {
        /**
         * **Resource ref counting**: Returns result with a contentRef reference that must be released after use.
         */
        fun getKotlinBinaryClassOrClassFileContent(
            file: VirtualFile, fileContent: ReusableByteArray? = null
        ): KotlinClassFinder.Result? {
            if (file.extension != JavaClassFileType.INSTANCE.defaultExtension &&
                file.fileType !== JavaClassFileType.INSTANCE
            ) return null

            if (file.name == PsiJavaModule.MODULE_INFO_CLS_FILE) return null

            val service: KotlinBinaryClassCache = ServiceManager.getService(KotlinBinaryClassCache::class.java)
            val requestCache: RequestCache = service.cache.get()

            if (file.modificationStamp == requestCache.modificationStamp && file == requestCache.virtualFile) {
                return requestCache.result!!.also { it.contentRef?.addRef() }
            }

            // Clear cache first, so that the previous byte array can be reused
            requestCache.clear()

            val aClass = ApplicationManager.getApplication().runReadAction(Computable {
                VirtualFileKotlinClass.create(file, fileContent)
            })

            return requestCache.cache(file, aClass)
        }
    }
}
