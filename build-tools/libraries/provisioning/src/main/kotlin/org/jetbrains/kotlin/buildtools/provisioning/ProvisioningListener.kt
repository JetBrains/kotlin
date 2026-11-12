/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.net.URI
import java.nio.file.Path

/**
 * Receives events from the library, which does not log on its own. An integration turns them into log lines, progress or
 * statistics. Every method does nothing by default.
 *
 * Methods are called from the thread doing the work and must not throw.
 */
public interface ProvisioningListener {
    /**
     * Another thread or process is installing [id], and this caller waits for [lockFile]. [holder] is the process that
     * holds the lock, as `pid@host`, if the lock file names one.
     */
    public fun waitingForLock(id: String, lockFile: Path, holder: String?) {}

    /** A directory without a marker file was deleted to make room for an installation. */
    public fun leftoverRemoved(installation: Path) {}

    /** [id] has been installed into [installation] by this caller. */
    public fun installed(id: String, installation: Path) {}

    /**
     * A download from [url] starts receiving bytes. [totalBytes] is the size of the whole file when the server announced it.
     * Called once per attempt.
     */
    public fun downloadStarted(url: URI, totalBytes: Long?) {}

    /** The file from [url] has [bytes] bytes on disk so far, counting those of an earlier attempt it continues. */
    public fun downloadProgress(url: URI, bytes: Long, totalBytes: Long?) {}

    /** The file from [url] is complete and verified: [bytes] bytes, of which this attempt took [durationMillis]. */
    public fun downloadFinished(url: URI, bytes: Long, durationMillis: Long) {}

    /** Attempt number [attempt] to download from [url] failed with [cause], and the downloader will try again. */
    public fun downloadRetried(url: URI, attempt: Int, cause: IOException) {}

    /** [archive] is being unpacked into [destination]. */
    public fun extractionStarted(archive: Path, destination: Path) {}

    public companion object {
        /** A listener that ignores every event. */
        public val None: ProvisioningListener = object : ProvisioningListener {}
    }
}
