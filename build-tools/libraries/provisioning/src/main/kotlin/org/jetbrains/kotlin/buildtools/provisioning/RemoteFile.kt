/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.net.URI
import java.util.Locale

/**
 * A file to download.
 *
 * @property urls where to get it, tried in order. Each URL is absolute.
 * @property sha256 the expected SHA-256 of the file as 64 hex digits, or null to skip the check. Stored in lower case.
 */
public class RemoteFile(urls: List<URI>, sha256: String? = null) {
    public val urls: List<URI> = urls.toList()

    public val sha256: String? = sha256?.lowercase(Locale.ROOT)

    /** A file available at a single [url]. */
    public constructor(url: URI, sha256: String? = null) : this(listOf(url), sha256)

    init {
        require(this.urls.isNotEmpty()) { "A remote file needs at least one URL" }
        this.urls.forEach { require(it.isAbsolute) { "A remote file URL must be absolute, got '$it'" } }
        require(this.sha256 == null || SHA256_PATTERN.matches(this.sha256)) {
            "A SHA-256 checksum must be 64 hex digits, got '$sha256'"
        }
    }

    override fun toString(): String = urls.joinToString(prefix = "RemoteFile(", postfix = ")")

    private companion object {
        val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

/**
 * How often [Downloader] tries one URL before it moves on to the next one, and how long it waits in between. The wait doubles
 * after each failed attempt, up to [maxDelayMillis].
 *
 * @property maxAttempts attempts per URL, at least 1.
 * @property initialDelayMillis the wait after the first failed attempt.
 * @property maxDelayMillis the longest wait.
 */
public class RetryPolicy(
    public val maxAttempts: Int = 3,
    public val initialDelayMillis: Long = 1_000,
    public val maxDelayMillis: Long = 10_000,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1, got $maxAttempts" }
        require(initialDelayMillis >= 0) { "initialDelayMillis must not be negative, got $initialDelayMillis" }
        require(maxDelayMillis >= initialDelayMillis) {
            "maxDelayMillis ($maxDelayMillis) must not be less than initialDelayMillis ($initialDelayMillis)"
        }
    }

    /** The wait after [failedAttempt], counted from 1. */
    internal fun delayAfter(failedAttempt: Int): Long {
        val doublings = (failedAttempt - 1).coerceIn(0, 30)
        // compared this way round, nothing overflows
        return if (initialDelayMillis > (maxDelayMillis shr doublings)) maxDelayMillis else initialDelayMillis shl doublings
    }
}
