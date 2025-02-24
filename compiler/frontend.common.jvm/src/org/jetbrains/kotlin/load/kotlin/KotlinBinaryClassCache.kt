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
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.PrintingLogger
import java.io.File
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class KotlinBinaryClassCache : Disposable {
    private val cache: ConcurrentHashMap<Pair<String, Long>, KotlinClassFinder.Result?> = ConcurrentHashMap()

    override fun dispose() {
        cache.clear()
        MY_PERF_LOGGER.info("KotlinBinaryClassCache is disposed")
    }

    companion object {
        var count: AtomicInteger = AtomicInteger(0)

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

            val currentThreadId = Thread.currentThread().id

            val application = ApplicationManager.getApplication()
            val service = application.getService(KotlinBinaryClassCache::class.java)
            val cache = service.cache

            val isExceptionsClass = file.toString().contains("kotlin${File.separator}ExceptionsKt.class")
            if (isExceptionsClass) {
                MY_PERF_LOGGER.info("${file.url} is accessed, metadata: $metadataVersion current thread id is $currentThreadId;")
            }

            return cache.getOrPut(file.url to file.modificationStamp) {
                if (isExceptionsClass) {
                    MY_PERF_LOGGER.info("${file.url} is created, metadata: $metadataVersion, current thread id is $currentThreadId;")
                }

                application.runReadAction(Computable {
                    VirtualFileKotlinClass.create(file, metadataVersion, fileContent, perfManager)
                }).also {
                    if (count.incrementAndGet() % 32 == 0) {
                        val totalSizeInBytes = cache.values.fold(0L) { acc, result ->
                            acc + when (result) {
                                is KotlinClass -> result.byteContent?.size ?: 0
                                is KotlinClassFinder.Result.ClassFileContent -> result.content.size
                                null -> 0
                            }
                        }
                        MY_PERF_LOGGER.info("Total size of KotlinBinaryClassCache is ${totalSizeInBytes / 1024} KB")
                    }
                }
            }
        }
    }
}

val MY_PERF_LOGGER = PrintingLogger(PrintStream(
        if (System.getProperty("os.name").contains("Mac")) {
            "/Users/Ivan.Kochurkin/Documents/JetBrains/logs/perf.info.log"
        } else {
            "F:\\JetBrains\\logs\\test.log"
        }
    ))
