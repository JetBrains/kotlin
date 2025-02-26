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
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.util.PerformanceManager
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

class KotlinBinaryClassCache : Disposable {
    // This global cache can be used only for `FileThatCanBeCachedAcrossApp` files (for instance, `FastJarVirtualFile`).
    // It's assumed they have properly implemented `equals` and `hashCode` methods that return `false` for modified files.
    // For instance, instances of `KotlinLocalVirtualFile` should be different for the same path if the real file was changed in any way.
    private val globalCache = ContainerUtil.createConcurrentSoftKeySoftValueMap<VirtualFile, KotlinClassFinder.Result>()

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
        globalCache.clear()
    }

    companion object {
        @Deprecated(
            "Please pass metadataVersion explicitly",
            ReplaceWith(
                "getKotlinBinaryClassOrClassFileContent(file, MetadataVersion.INSTANCE, fileContent, perfManager)",
                "org.jetbrains.kotlin.metadata.deserialization.MetadataVersion"
            )
        )
        fun getKotlinBinaryClassOrClassFileContent(
            file: VirtualFile, fileContent: ByteArray?
        ) = getKotlinBinaryClassOrClassFileContent(
            file,
            metadataVersion = MetadataVersion.INSTANCE,
            fileContent = fileContent,
            perfManager = null,
        )

        fun getKotlinBinaryClassOrClassFileContent(
            file: VirtualFile,
            metadataVersion: MetadataVersion,
            fileContent: ByteArray? = null,
            perfManager: PerformanceManager? = null, // The parameter has `null` default to prevent fixing external code (IntelliJ)
        ): KotlinClassFinder.Result? {
            if (file.extension != JavaClassFileType.INSTANCE.defaultExtension &&
                file.fileType !== JavaClassFileType.INSTANCE
            ) return null

            if (file.name == PsiJavaModule.MODULE_INFO_CLS_FILE) return null

            val application = ApplicationManager.getApplication()
            val service = application.getService(KotlinBinaryClassCache::class.java)

            fun createKotlinClass(): KotlinClassFinder.Result = application.runReadAction(Computable {
                VirtualFileKotlinClass.create(file, metadataVersion, fileContent, perfManager)
            })

            if (file is FileThatCanBeCachedAcrossEntireApp) {
                return service.globalCache.getOrPut(file) { createKotlinClass() }.takeIf { it !is KotlinClassFinder.Result.Empty }
            } else {
                val requestCache = service.cache.get()

                if (file.modificationStamp == requestCache.modificationStamp && file == requestCache.virtualFile) {
                    return requestCache.result
                }

                val aClass = createKotlinClass().takeIf { it !is KotlinClassFinder.Result.Empty }

                return requestCache.cache(file, aClass)
            }
        }
    }
}