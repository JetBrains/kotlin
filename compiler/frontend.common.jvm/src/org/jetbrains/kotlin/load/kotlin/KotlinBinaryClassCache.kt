/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

class KotlinBinaryClassCache : Disposable {
    private val requestCaches = CopyOnWriteArrayList<WeakReference<RequestCache>>()

    private class RequestCache {
        var virtualFile: VirtualFile? = null
        var modificationStamp: Long = 0
        var result: KotlinClassFinder.Result? = null

        fun cache(
            file: VirtualFile,
            result: KotlinClassFinder.Result?
        ): KotlinClassFinder.Result? {
            virtualFile = file
            this.result = result
            modificationStamp = file.modificationStamp

            return result
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
            cache.get()?.run {
                result = null
                virtualFile = null
            }
        }
        requestCaches.clear()
        // This is only relevant for tests. We create a new instance of Application for each test, and so a new instance of this service is
        // also created for each test. However all tests share the same event dispatch thread, which would collect all instances of this
        // thread-local if they're not removed properly. Each instance would transitively retain VFS resulting in OutOfMemoryError
        cache.remove()
    }

    companion object {
        @Deprecated(
            "Please pass jvmMetadataVersion explicitly",
            ReplaceWith(
                "getKotlinBinaryClassOrClassFileContent(file, JvmMetadataVersion.INSTANCE, fileContent = fileContent)",
                "org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion"
            )
        )
        fun getKotlinBinaryClassOrClassFileContent(
            file: VirtualFile, fileContent: ByteArray?
        ) = getKotlinBinaryClassOrClassFileContent(file, jvmMetadataVersion = JvmMetadataVersion.INSTANCE, fileContent = fileContent)

        fun getKotlinBinaryClassOrClassFileContent(
            file: VirtualFile, jvmMetadataVersion: JvmMetadataVersion, fileContent: ByteArray? = null
        ): KotlinClassFinder.Result? {
            if (file.extension != JavaClassFileType.INSTANCE.defaultExtension &&
                file.fileType !== JavaClassFileType.INSTANCE
            ) return null

            if (file.name == PsiJavaModule.MODULE_INFO_CLS_FILE) return null

            val service = ApplicationManager.getApplication().getService(KotlinBinaryClassCache::class.java)
            val requestCache = service.cache.get()

            if (file.modificationStamp == requestCache.modificationStamp && file == requestCache.virtualFile) {
                return requestCache.result
            }

            val aClass = ApplicationManager.getApplication().runReadAction(Computable {
                VirtualFileKotlinClass.create(file, jvmMetadataVersion, fileContent)
            })

            return requestCache.cache(file, aClass)
        }
    }
}
