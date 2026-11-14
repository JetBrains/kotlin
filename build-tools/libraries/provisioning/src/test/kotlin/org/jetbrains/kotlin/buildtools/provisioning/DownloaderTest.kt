/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DownloaderTest {

    @TempDir
    lateinit var tempDir: Path

    private val target: Path get() = tempDir.resolve("tool-1.0.tar.gz")
    private val part: Path get() = tempDir.resolve("tool-1.0.tar.gz.part")
    private val validatorFile: Path get() = tempDir.resolve("tool-1.0.tar.gz.part.validator")

    private val content = ByteArray(10) { it.toByte() }
    private val url = URI("https://example.org/tool-1.0.tar.gz")
    private val mirror = URI("https://mirror.example.org/tool-1.0.tar.gz")

    private val events = mutableListOf<String>()
    private val sleeps = mutableListOf<Long>()

    private val listener = object : ProvisioningListener {
        override fun downloadStarted(url: URI, totalBytes: Long?) {
            events += "started ${url.host} $totalBytes"
        }

        override fun downloadProgress(url: URI, bytes: Long, totalBytes: Long?) {
            events += "progress ${url.host} $bytes/$totalBytes"
        }

        override fun downloadFinished(url: URI, bytes: Long, durationMillis: Long) {
            events += "finished ${url.host} $bytes in $durationMillis"
        }

        override fun downloadRetried(url: URI, attempt: Int, cause: IOException) {
            events += "retried ${url.host} $attempt: ${cause.message}"
        }
    }

    /** Answers requests in order and records them. */
    private class ScriptedTransport(vararg answers: (DownloadRequest) -> DownloadResponse) : DownloadTransport {
        private val answers = answers.toMutableList()
        val requests = mutableListOf<String>()

        override fun open(request: DownloadRequest): DownloadResponse {
            requests += "${request.url.host} from ${request.offset} if ${request.ifRange}"
            check(answers.isNotEmpty()) { "unexpected request $request" }
            return answers.removeAt(0)(request)
        }
    }

    private fun downloader(transport: DownloadTransport, offline: Boolean = false, retry: RetryPolicy = RetryPolicy()): Downloader {
        var clock = 0L
        return Downloader(offline, retry, transport, listener, sleep = { sleeps += it }, nanoTime = { clock += 5_000_000; clock })
    }

    private fun whole(bytes: ByteArray = content, etag: String? = "\"v1\""): (DownloadRequest) -> DownloadResponse = {
        DownloadResponse(200, listOfNotNull("Content-Length" to bytes.size.toString(), etag?.let { "ETag" to it }).toMap(), ByteArrayInputStream(bytes))
    }

    private fun range(from: Int, bytes: ByteArray = content): (DownloadRequest) -> DownloadResponse = {
        DownloadResponse(
            206,
            mapOf("Content-Range" to "bytes $from-${bytes.size - 1}/${bytes.size}", "Content-Length" to (bytes.size - from).toString()),
            ByteArrayInputStream(bytes, from, bytes.size - from),
        )
    }

    private fun status(code: Int): (DownloadRequest) -> DownloadResponse = { DownloadResponse(code, emptyMap(), ByteArrayInputStream(ByteArray(0))) }

    /** A response announcing the whole file that breaks after [breakAfter] bytes. */
    private fun dropsAfter(breakAfter: Int, etag: String? = "\"v1\""): (DownloadRequest) -> DownloadResponse = {
        DownloadResponse(
            200,
            listOfNotNull("Content-Length" to content.size.toString(), etag?.let { "ETag" to it }).toMap(),
            BreakingStream(content.copyOf(breakAfter)),
        )
    }

    private class BreakingStream(bytes: ByteArray) : InputStream() {
        private val data = ByteArrayInputStream(bytes)
        override fun read(): Int = data.read().also { if (it < 0) throw SocketTimeoutException("Read timed out") }
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            data.read(b, off, len).also { if (it < 0) throw SocketTimeoutException("Read timed out") }
    }

    @Test
    fun `downloads a file`() {
        val transport = ScriptedTransport(whole())

        assertEquals(target, downloader(transport).download(RemoteFile(url), target))

        assertContentEquals(content, Files.readAllBytes(target))
        assertFalse(Files.exists(part))
        assertFalse(Files.exists(validatorFile))
        assertEquals(listOf("example.org from 0 if null"), transport.requests)
        assertEquals(listOf("started example.org 10", "finished example.org 10 in 5"), events)
    }

    @Test
    fun `replaces a file that is already there`() {
        Files.write(target, byteArrayOf(42))

        downloader(ScriptedTransport(whole())).download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
    }

    @Test
    fun `creates the parent directories of the target`() {
        val nested = tempDir.resolve("a b/ü/tool.tar.gz")

        downloader(ScriptedTransport(whole())).download(RemoteFile(url), nested)

        assertContentEquals(content, Files.readAllBytes(nested))
    }

    @Test
    fun `accepts a matching checksum in any case`() {
        downloader(ScriptedTransport(whole())).download(RemoteFile(url, sha256(content).uppercase()), target)

        assertContentEquals(content, Files.readAllBytes(target))
    }

    @Test
    fun `a checksum mismatch is not retried and leaves nothing behind`() {
        val expected = "0".repeat(64)
        val transport = ScriptedTransport(whole())

        val exception = assertFailsWith<ChecksumMismatchException> {
            downloader(transport).download(RemoteFile(url, expected), target)
        }

        assertEquals(expected, exception.expected)
        assertEquals(sha256(content), exception.actual)
        assertEquals(
            "The file downloaded from $url has the SHA-256 checksum ${sha256(content)}, but $expected was expected. " +
                    "Either the file on the server or the expected checksum is wrong.",
            exception.message,
        )
        assertEquals(1, transport.requests.size, "a checksum mismatch is not retried")
        assertFalse(Files.exists(target))
        assertFalse(Files.exists(part))
        assertFalse(Files.exists(validatorFile))
    }

    @Test
    fun `continues a dropped download from where it stopped`() {
        val transport = ScriptedTransport(dropsAfter(4), range(from = 4))

        downloader(transport).download(RemoteFile(url, sha256(content)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 0 if null", "example.org from 4 if \"v1\""), transport.requests)
        assertEquals(listOf(1_000L), sleeps)
        assertEquals(
            listOf(
                "started example.org 10",
                "retried example.org 1: Read timed out",
                "started example.org 10",
                "finished example.org 10 in 5",
            ),
            events,
        )
    }

    @Test
    fun `a checksum mismatch on one URL moves on to the next`() {
        // a captive portal, for example, answers 200 with a page of its own
        val transport = ScriptedTransport(whole("<html>log in</html>".toByteArray()), whole())

        downloader(transport).download(RemoteFile(listOf(url, mirror), sha256(content)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 0 if null", "mirror.example.org from 0 if null"), transport.requests)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `old bytes are gone before the validator of a changed file is kept`() {
        Files.write(part, content.copyOf(6))
        Files.write(validatorFile, "$url\n\"v1\"".toByteArray())
        val interrupted = IllegalStateException("interrupted right after the response arrived")
        val interruptingListener = object : ProvisioningListener {
            override fun downloadStarted(url: URI, totalBytes: Long?) = throw interrupted
        }
        val downloader = Downloader(
            false, RetryPolicy(), ScriptedTransport(whole(ByteArray(12), etag = "\"v2\"")), interruptingListener, sleep = {}, nanoTime = { 0 },
        )

        assertSame(interrupted, assertFailsWith<IllegalStateException> { downloader.download(RemoteFile(url), target) })

        // otherwise the next attempt would append bytes of the new file to bytes of the old one
        assertFalse(Files.exists(part))
        assertEquals("$url\n\"v2\"", String(Files.readAllBytes(validatorFile)))
    }

    @Test
    fun `bytes from one URL are not continued from another`() {
        val transport = ScriptedTransport(dropsAfter(4), whole())

        downloader(transport, retry = RetryPolicy(maxAttempts = 1)).download(RemoteFile(listOf(url, mirror)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        // the mirror may have another build of the file under the same validator
        assertEquals(listOf("example.org from 0 if null", "mirror.example.org from 0 if null"), transport.requests)
    }

    @Test
    fun `a range of a file that has changed makes it start over`() {
        // a server that honours Range and ignores If-Range
        val changed = ByteArray(10) { (50 + it).toByte() }
        val rangeOfChangedFile: (DownloadRequest) -> DownloadResponse = {
            DownloadResponse(206, mapOf("Content-Range" to "bytes 4-9/10", "ETag" to "\"v2\""), ByteArrayInputStream(changed, 4, 6))
        }
        val transport = ScriptedTransport(dropsAfter(4), rangeOfChangedFile, whole(changed, etag = "\"v2\""))

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(changed, Files.readAllBytes(target))
        assertEquals("retried example.org 2: $url sent a range of a file that has changed since the download began", events[2])
        assertEquals("example.org from 0 if null", transport.requests[2])
    }

    @Test
    fun `a continued download with a wrong checksum is downloaded again from scratch`() {
        // the bytes left on disk by an earlier build are damaged, the server's file is fine
        Files.write(part, ByteArray(6))
        Files.write(validatorFile, "$url\n\"v1\"".toByteArray())
        val transport = ScriptedTransport(range(from = 6), whole())

        downloader(transport).download(RemoteFile(url, sha256(content)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 6 if \"v1\"", "example.org from 0 if null"), transport.requests)
        assertEquals("retried example.org 1: $url: the continued download does not match the checksum", events[1])
    }

    @Test
    fun `a failure of the local file system is not retried`() {
        // a directory with something in it where the part file belongs cannot be replaced
        Files.write(Files.createDirectories(part).resolve("in the way"), byteArrayOf(1))
        val transport = ScriptedTransport(whole())

        assertFailsWith<DirectoryNotEmptyException> { downloader(transport).download(RemoteFile(listOf(url, mirror)), target) }

        assertEquals(1, transport.requests.size)
        assertTrue(sleeps.isEmpty())
        assertTrue(events.none { it.startsWith("retried") })
    }

    @Test
    fun `a resumed body shorter than its range is retried when the total is unknown`() {
        val shortRange: (DownloadRequest) -> DownloadResponse = {
            DownloadResponse(206, mapOf("Content-Range" to "bytes 4-9/*"), ByteArrayInputStream(content, 4, 3))
        }
        val restOfRange: (DownloadRequest) -> DownloadResponse = {
            DownloadResponse(206, mapOf("Content-Range" to "bytes 7-9/*"), ByteArrayInputStream(content, 7, 3))
        }
        val transport = ScriptedTransport(dropsAfter(4), shortRange, restOfRange)

        downloader(transport).download(RemoteFile(url, sha256(content)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals("retried example.org 2: $url ended after 7 of 10 bytes", events[3])
    }

    @Test
    fun `starts over when the server sends the whole file instead of the range`() {
        // a server without range support, or a file that changed since the first attempt
        val changed = ByteArray(12) { (100 + it).toByte() }
        val transport = ScriptedTransport(dropsAfter(4), whole(changed, etag = "\"v2\""))

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(changed, Files.readAllBytes(target))
        assertEquals("example.org from 4 if \"v1\"", transport.requests[1])
    }

    @Test
    fun `continues a part file left by an earlier build`() {
        Files.write(part, content.copyOf(6))
        Files.write(validatorFile, "$url\n\"v1\"".toByteArray())
        val transport = ScriptedTransport(range(from = 6))

        downloader(transport).download(RemoteFile(url, sha256(content)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 6 if \"v1\""), transport.requests)
    }

    @Test
    fun `does not continue a part file without a validator`() {
        Files.write(part, byteArrayOf(9, 9, 9))
        val transport = ScriptedTransport(whole())

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 0 if null"), transport.requests)
    }

    @Test
    fun `does not continue when the server gave no validator`() {
        val transport = ScriptedTransport(dropsAfter(4, etag = null), whole(etag = null))

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals("example.org from 0 if null", transport.requests[1])
    }

    @Test
    fun `uses Last-Modified when there is no ETag`() {
        val lastModified = "Wed, 21 Oct 2026 07:28:00 GMT"
        val transport = ScriptedTransport(
            {
                DownloadResponse(200, mapOf("Content-Length" to "10", "Last-Modified" to lastModified), BreakingStream(content.copyOf(3)))
            },
            range(from = 3),
        )

        downloader(transport).download(RemoteFile(url), target)

        assertEquals("example.org from 3 if $lastModified", transport.requests[1])
    }

    @Test
    fun `a body shorter than announced is retried`() {
        val transport = ScriptedTransport(
            { DownloadResponse(200, mapOf("Content-Length" to "10", "ETag" to "\"v1\""), ByteArrayInputStream(content, 0, 7)) },
            range(from = 7),
        )

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals("retried example.org 1: $url ended after 7 of 10 bytes", events[1])
    }

    @Test
    fun `a range that does not start where asked makes it start over`() {
        val transport = ScriptedTransport(dropsAfter(4), range(from = 2), whole())

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        // the rejected range never started a download, so the second retry follows the first directly
        assertEquals("retried example.org 2: $url sent the range 'bytes 2-9/10' for a request from byte 4", events[2])
        assertEquals("example.org from 0 if null", transport.requests[2])
    }

    @Test
    fun `range not satisfiable makes it start over`() {
        Files.write(part, content)
        Files.write(validatorFile, "$url\n\"v1\"".toByteArray())
        val transport = ScriptedTransport(status(416), whole())

        downloader(transport).download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 10 if \"v1\"", "example.org from 0 if null"), transport.requests)
    }

    @Test
    fun `server errors and throttling are retried with a growing delay`() {
        val transport = ScriptedTransport(status(503), status(429), whole())

        downloader(transport, retry = RetryPolicy(maxAttempts = 3, initialDelayMillis = 100, maxDelayMillis = 150))
            .download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf(100L, 150L), sleeps)
    }

    @Test
    fun `a client error is not retried and the next URL is tried`() {
        val transport = ScriptedTransport(status(404), whole())

        downloader(transport).download(RemoteFile(listOf(url, mirror)), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals(listOf("example.org from 0 if null", "mirror.example.org from 0 if null"), transport.requests)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `fails after every attempt on every URL`() {
        val transport = ScriptedTransport(status(500), status(500), status(403))

        val exception = assertFailsWith<DownloadFailedException> {
            downloader(transport, retry = RetryPolicy(maxAttempts = 2)).download(RemoteFile(listOf(url, mirror)), target)
        }

        assertEquals(listOf(url, mirror), exception.urls)
        assertEquals(
            "Cannot download 'tool-1.0.tar.gz' from $url, $mirror: $mirror answered with HTTP status 403. " +
                    "Check the network connection, proxy settings and the URL.",
            exception.message,
        )
        assertIs<IOException>(exception.cause)
        assertEquals(3, transport.requests.size)
        assertFalse(Files.exists(target))
    }

    @Test
    fun `keeps what it got when it gives up, so the next build can continue`() {
        val transport = ScriptedTransport(dropsAfter(4))

        assertFailsWith<DownloadFailedException> {
            downloader(transport, retry = RetryPolicy(maxAttempts = 1)).download(RemoteFile(url), target)
        }

        assertContentEquals(content.copyOf(4), Files.readAllBytes(part))
        assertEquals("$url\n\"v1\"", String(Files.readAllBytes(validatorFile)))
    }

    @Test
    fun `an exception that is not an IOException is not retried`() {
        val failure = IllegalStateException("broken transport")
        val transport = DownloadTransport { throw failure }

        assertSame(failure, assertFailsWith<IllegalStateException> { downloader(transport).download(RemoteFile(url), target) })
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `offline mode does not download`() {
        val transport = ScriptedTransport()

        val exception = assertFailsWith<OfflineException> { downloader(transport, offline = true).download(RemoteFile(url), target) }

        assertEquals(listOf(url), exception.urls)
        assertEquals(
            "Cannot download 'tool-1.0.tar.gz' from $url: downloading is disabled in offline mode. Run the build once without offline mode.",
            exception.message,
        )
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun `reports progress every megabyte`() {
        val big = ByteArray(5 * 1024 * 1024 / 2) { it.toByte() }

        downloader(ScriptedTransport(whole(big))).download(RemoteFile(url), target)

        assertEquals(
            listOf(
                "started example.org ${big.size}",
                "progress example.org 1048576/${big.size}",
                "progress example.org 2097152/${big.size}",
                "finished example.org ${big.size} in 5",
            ),
            events,
        )
    }

    @Test
    fun `a missing length is not checked`() {
        downloader(ScriptedTransport({ DownloadResponse(200, emptyMap(), ByteArrayInputStream(content)) }))
            .download(RemoteFile(url), target)

        assertContentEquals(content, Files.readAllBytes(target))
        assertEquals("started example.org null", events.first())
    }

    @Test
    fun `remote files are validated`() {
        assertEquals("A remote file needs at least one URL", assertFailsWith<IllegalArgumentException> { RemoteFile(emptyList()) }.message)
        assertEquals(
            "A remote file URL must be absolute, got 'tool.tar.gz'",
            assertFailsWith<IllegalArgumentException> { RemoteFile(URI("tool.tar.gz")) }.message,
        )
        assertEquals(
            "A SHA-256 checksum must be 64 hex digits, got 'abc'",
            assertFailsWith<IllegalArgumentException> { RemoteFile(url, "abc") }.message,
        )
        assertNull(RemoteFile(url).sha256)
    }

    @Test
    fun `the retry delay doubles up to the maximum`() {
        val policy = RetryPolicy(maxAttempts = 10, initialDelayMillis = 1_000, maxDelayMillis = 5_000)
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L), (1..5).map(policy::delayAfter))
        assertEquals(Long.MAX_VALUE, RetryPolicy(10, Long.MAX_VALUE / 2, Long.MAX_VALUE).delayAfter(40))
        assertFailsWith<IllegalArgumentException> { RetryPolicy(maxAttempts = 0) }
        assertFailsWith<IllegalArgumentException> { RetryPolicy(initialDelayMillis = 10, maxDelayMillis = 5) }
    }

    @Test
    fun `parses content ranges`() {
        assertEquals(Triple(4L, 9L, 10L), parseContentRange("bytes 4-9/10")?.let { Triple(it.first, it.last, it.total) })
        assertNull(parseContentRange("bytes 4-9/*")?.total)
        val tooLong = "9".repeat(30)
        for (invalid in listOf(null, "", "bytes 9-4/10", "bytes 4-10/10", "items 4-9/10", "bytes */10", "bytes 0-$tooLong/*", "bytes 0-9/$tooLong")) {
            assertNull(parseContentRange(invalid), "'$invalid'")
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { String.format("%02x", it) }
}
