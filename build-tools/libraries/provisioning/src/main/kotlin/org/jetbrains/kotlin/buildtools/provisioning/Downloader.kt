/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

private const val PART_SUFFIX = ".part"
private const val VALIDATOR_SUFFIX = ".part.validator"
private const val BUFFER_SIZE = 64 * 1024
private const val PROGRESS_STEP_BYTES = 1L shl 20

/**
 * Downloads files, resuming and retrying when the connection fails.
 *
 * Bytes go to `<target>.part` first, and the target name appears only when the file is complete and verified, so a file with
 * the target name is never partial. A `.part` file left by an earlier failure is continued from where it stopped, provided the
 * server supports ranges and the file has not changed on the server since. For that, `<target>.part.validator` keeps the URL
 * the bytes came from and the server's `ETag` or `Last-Modified`; bytes from one URL are never continued from another.
 *
 * Connection failures, timeouts, bodies shorter than announced and the statuses 429 and 5xx are retried according to the
 * [RetryPolicy]. Other statuses, such as 404, and a wrong checksum are not: the next URL is tried instead. Failures of the
 * local file system, a full disk for example, are not retried at all and reach the caller as they are.
 *
 * The downloader does not lock: two downloads to the same target at the same time are not supported. Call it from
 * [InstallationStore.install], whose lock makes sure one caller downloads a given distribution.
 *
 * @param offline when true, nothing is downloaded and [download] throws [OfflineException].
 * @param retry how often to try each URL.
 * @param transport opens the URLs.
 * @param listener receives download events.
 */
public class Downloader internal constructor(
    private val offline: Boolean,
    private val retry: RetryPolicy,
    private val transport: DownloadTransport,
    private val listener: ProvisioningListener,
    private val sleep: (millis: Long) -> Unit,
    private val nanoTime: () -> Long,
) {
    public constructor(
        offline: Boolean = false,
        retry: RetryPolicy = RetryPolicy(),
        transport: DownloadTransport = HttpTransport(),
        listener: ProvisioningListener = ProvisioningListener.None,
    ) : this(offline, retry, transport, listener, Thread::sleep, System::nanoTime)

    /**
     * Downloads [file] to [target], replacing a file that is already there, and returns [target]. Creates the parent directories.
     *
     * @throws OfflineException if the downloader is offline.
     * @throws ChecksumMismatchException if no URL worked and the last one sent bytes that do not match [RemoteFile.sha256].
     * @throws DownloadFailedException if every attempt on every URL failed for another reason.
     */
    public fun download(file: RemoteFile, target: Path): Path {
        if (offline) throw OfflineException(file.urls, target)

        val absoluteTarget = target.toAbsolutePath().normalize()
        absoluteTarget.parent?.let { Files.createDirectories(it) }
        val fileName = absoluteTarget.fileName.toString()
        val part = absoluteTarget.resolveSibling(fileName + PART_SUFFIX)
        val validatorFile = absoluteTarget.resolveSibling(fileName + VALIDATOR_SUFFIX)

        var lastFailure: IOException? = null
        for (url in file.urls) {
            for (attempt in 1..retry.maxAttempts) {
                try {
                    fetch(url, file.sha256, part, validatorFile)
                } catch (e: ChecksumMismatchException) {
                    // the same URL would send the same bytes again; a mirror may have the right ones
                    lastFailure = e
                    break
                } catch (e: TransferException) {
                    lastFailure = e.failure
                    if (!e.isRetryable) break
                    if (attempt < retry.maxAttempts) {
                        listener.downloadRetried(url, attempt, e.failure)
                        sleep(retry.delayAfter(attempt))
                    }
                    continue
                }
                Files.move(part, absoluteTarget, StandardCopyOption.REPLACE_EXISTING)
                Files.deleteIfExists(validatorFile)
                return target
            }
        }
        val failure = checkNotNull(lastFailure)
        throw failure as? ChecksumMismatchException ?: DownloadFailedException(file.urls, target, failure)
    }

    /**
     * Makes [part] the complete, verified file from [url], continuing what it already holds when possible.
     *
     * @throws TransferException when the server or the connection failed. Any other [IOException] is a local failure.
     */
    private fun fetch(url: URI, sha256: String?, part: Path, validatorFile: Path) {
        val existingBytes = if (Files.isRegularFile(part)) Files.size(part) else 0L
        // Without a validator from this very URL there is no way to know that the bytes on disk are the start of the
        // file the server has now.
        val validator = if (existingBytes > 0) readValidator(validatorFile, url) else null
        val offset = if (validator != null) existingBytes else 0L
        val started = nanoTime()

        transfer { transport.open(DownloadRequest(url, offset, validator)) }.use { response ->
            val resumed = when (response.status) {
                200 -> false
                206 -> true
                416 -> {
                    discard(part, validatorFile)
                    throw unexpectedStatus(url, response.status, isRetryable = true)
                }
                else -> throw unexpectedStatus(url, response.status, isRetryable = response.status.isRetryableStatus())
            }

            val totalBytes: Long?
            val expectedBytes: Long?
            if (resumed) {
                val contentRange = response.header("Content-Range")
                val range = parseContentRange(contentRange)
                if (offset == 0L || range == null || range.first != offset) {
                    discard(part, validatorFile)
                    throw TransferException(IOException("$url sent the range '$contentRange' for a request from byte $offset"))
                }
                // A server may honour Range and ignore If-Range. Its own validator then tells that the file has changed.
                val currentValidator = response.header("ETag") ?: response.header("Last-Modified")
                if (currentValidator != null && currentValidator != validator) {
                    discard(part, validatorFile)
                    throw TransferException(IOException("$url sent a range of a file that has changed since the download began"))
                }
                totalBytes = range.total
                // without a total, the end of the range is all there is to check the body against
                expectedBytes = range.total ?: (range.last + 1)
            } else {
                // The old bytes go first. If this stops halfway, the new validator must not end up next to bytes of the old file.
                discard(part, validatorFile)
                val newValidator = response.header("ETag") ?: response.header("Last-Modified")
                if (newValidator != null) Files.write(validatorFile, "$url\n$newValidator".toByteArray(Charsets.UTF_8))
                totalBytes = response.header("Content-Length")?.toLongOrNull()
                expectedBytes = totalBytes
            }

            val digest = if (sha256 != null) MessageDigest.getInstance("SHA-256") else null
            if (resumed && digest != null) digest.updateFrom(part)

            listener.downloadStarted(url, totalBytes)
            var bytes = if (resumed) offset else 0L
            var nextProgress = bytes + PROGRESS_STEP_BYTES
            val openOptions = if (resumed) {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
            } else {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            }
            Files.newOutputStream(part, *openOptions).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = transfer { response.body.read(buffer) }
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest?.update(buffer, 0, read)
                    bytes += read
                    if (bytes >= nextProgress) {
                        listener.downloadProgress(url, bytes, totalBytes)
                        nextProgress = bytes + PROGRESS_STEP_BYTES
                    }
                }
            }

            if (expectedBytes != null && bytes != expectedBytes) {
                // a longer file cannot be continued; a shorter one can
                if (bytes > expectedBytes) discard(part, validatorFile)
                throw TransferException(IOException("$url ended after $bytes of $expectedBytes bytes"))
            }
            if (sha256 != null && digest != null) {
                val actual = digest.digest().toHex()
                if (actual != sha256) {
                    discard(part, validatorFile)
                    // Some of these bytes were on disk from an earlier attempt, and they may be the bad ones.
                    // A download from scratch settles whether the server has the right file.
                    if (resumed) throw TransferException(IOException("$url: the continued download does not match the checksum"))
                    throw ChecksumMismatchException(url, sha256, actual)
                }
            }
            listener.downloadFinished(url, bytes, (nanoTime() - started) / 1_000_000)
        }
    }

    private fun discard(part: Path, validatorFile: Path) {
        Files.deleteIfExists(part)
        Files.deleteIfExists(validatorFile)
    }

    /** The validator kept for the part file, if it was downloaded from [url]. */
    private fun readValidator(validatorFile: Path, url: URI): String? {
        if (!Files.isRegularFile(validatorFile)) return null
        val lines = String(Files.readAllBytes(validatorFile), Charsets.UTF_8).lines()
        return if (lines.size == 2 && lines[0] == url.toString()) lines[1].takeIf { it.isNotEmpty() } else null
    }
}

/**
 * The server or the connection failed, as opposed to the local file system. Only these failures are retried.
 *
 * @property failure what went wrong, as reported to the listener and the caller.
 * @property isRetryable false when the same request would fail the same way again.
 */
private class TransferException(val failure: IOException, val isRetryable: Boolean = true) : IOException(failure)

private inline fun <T> transfer(action: () -> T): T =
    try {
        action()
    } catch (e: IOException) {
        throw TransferException(e)
    }

private fun unexpectedStatus(url: URI, status: Int, isRetryable: Boolean) =
    TransferException(IOException("$url answered with HTTP status $status"), isRetryable)

/** A parsed `Content-Range: bytes <first>-<last>/<total>` header; [total] is null for `*`. */
internal class ContentRange(val first: Long, val last: Long, val total: Long?)

internal fun parseContentRange(header: String?): ContentRange? {
    val match = header?.let { CONTENT_RANGE.matchEntire(it.trim()) } ?: return null
    val (first, last, total) = match.destructured
    // the digits come from a server and may not fit into a Long
    val range = ContentRange(
        first.toLongOrNull() ?: return null,
        last.toLongOrNull() ?: return null,
        if (total == "*") null else total.toLongOrNull() ?: return null,
    )
    return range.takeIf { it.first <= it.last && (it.total == null || it.last < it.total) }
}

private val CONTENT_RANGE = Regex("""bytes (\d+)-(\d+)/(\d+|\*)""", RegexOption.IGNORE_CASE)

private fun Int.isRetryableStatus(): Boolean = this == 429 || this in 500..599

/** The SHA-256 of [file] as lower-case hex digits. */
internal fun sha256Of(file: Path): String = MessageDigest.getInstance("SHA-256").apply { updateFrom(file) }.digest().toHex()

private fun MessageDigest.updateFrom(file: Path) {
    Files.newInputStream(file).use { input ->
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            update(buffer, 0, read)
        }
    }
}

private fun ByteArray.toHex(): String = joinToString("") { String.format("%02x", it) }
