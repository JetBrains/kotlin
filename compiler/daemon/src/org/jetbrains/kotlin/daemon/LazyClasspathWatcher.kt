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

package org.jetbrains.kotlin.daemon

import java.io.File
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread


val CLASSPATH_FILE_ID_DIGEST = "MD5"
val DEFAULT_CLASSPATH_WATCH_PERIOD_MS = 1000L
val DEFAULT_CLASSPATH_DIGEST_WATCH_PERIOD_MS = 300000L // 5 min


/**
 * Class for lazy (on demand) check if any relevant file in the classpath is changed
 * poor-man watcher in the absence of NIO
 * TODO: replace with NIO watching when switching to java 7+
 */
class LazyClasspathWatcher(classpath: Iterable<String>,
                           val checkPeriod: Long = DEFAULT_CLASSPATH_WATCH_PERIOD_MS,
                           val digestCheckPeriod: Long = DEFAULT_CLASSPATH_DIGEST_WATCH_PERIOD_MS) {

    private data class FileId(val file: File, val lastModified: Long, val digest: ByteArray)

    private val fileIdsLock = Semaphore(1) // a barrier for ensuring ids are initialized, using semaphore to allow modifications from another thread
    private var fileIds: List<FileId>? = null
    private val lastChangedStatus = AtomicBoolean(false)
    private val lastUpdate = AtomicLong(0)
    private val lastDigestUpdate = AtomicLong(0)
    private val log by lazy { Logger.getLogger("classpath watcher") }

    init {
        // locking before entering thread in order to avoid racing with isChanged
        fileIdsLock.acquire()
        thread(isDaemon = true, start = true) {
            try {
                fileIds = classpath
                        .map(::File)
                        .asSequence()
                        .flatMap { it.walk().filter(::isClasspathFile) }
                        .map { FileId(it, it.lastModified(), it.md5Digest()) }
                        .toList()
                val nowMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
                lastUpdate.set(nowMs)
                lastDigestUpdate.set(nowMs)
            }
            catch (e: IOException) {
                log.log(Level.WARNING, "Error on walking classpath", e)
                // ignoring it for now
            }
            finally {
                fileIdsLock.release()
            }
        }
    }

    val isChanged: Boolean get() {
        if (lastChangedStatus.get()) return true
        val nowMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
        if (nowMs - lastUpdate.get() < checkPeriod) return false

        val checkDigest = nowMs - lastDigestUpdate.get() > digestCheckPeriod
        // making sure that fieldIds are initialized
        fileIdsLock.acquire()
        fileIdsLock.release()
        val changed =
            fileIds?.find {
                try {
                    if (!it.file.exists()) {
                        log.info("cp changed: ${it.file} doesn't exist any more")
                        true
                    }
                    // if last modified changed or if enforced by param - checking the digest
                    else if ((it.file.lastModified() != it.lastModified || checkDigest) && !Arrays.equals(it.digest, it.file.md5Digest())) {
                        log.info("cp changed: ${it.file} digests differ")
                        true
                    }
                    else false
                }
                catch (e: IOException) {
                    log.log(Level.INFO, "cp changed: ${it.file} access throws the exception", e)
                    true // io error considered as change
                }
            } != null
        lastUpdate.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()))
        if (checkDigest) lastDigestUpdate.set(lastUpdate.get())

        return changed
    }
}


fun isClasspathFile(file: File): Boolean = file.isFile && listOf("class", "jar").contains(file.extension.toLowerCase())

fun File.md5Digest(): ByteArray {
    val md = MessageDigest.getInstance(CLASSPATH_FILE_ID_DIGEST)
    DigestInputStream(inputStream(), md).use {
        val buf = ByteArray(1024)
        while (it.read(buf) != -1) {}
        it.close()
    }
    return md.digest()
}

