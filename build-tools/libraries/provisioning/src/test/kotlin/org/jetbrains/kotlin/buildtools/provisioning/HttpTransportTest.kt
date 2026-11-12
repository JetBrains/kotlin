/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Runs [HttpTransport] against a server on the loopback interface that the test starts itself, so no other network is used.
 */
class HttpTransportTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var server: HttpServer
    private val content = ByteArray(1000) { (it % 251).toByte() }
    private val receivedHeaders = Collections.synchronizedList(mutableListOf<String>())
    private val releaseSlowResponse = CountDownLatch(1)

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/file") { exchange -> exchange.respond { serveFile(it) } }
        server.createContext("/redirect") { exchange ->
            exchange.respond {
                it.responseHeaders.add("Location", "/file")
                it.sendResponseHeaders(302, -1)
            }
        }
        server.createContext("/redirect-loop") { exchange ->
            exchange.respond {
                it.responseHeaders.add("Location", "/redirect-loop")
                it.sendResponseHeaders(307, -1)
            }
        }
        server.createContext("/missing") { exchange ->
            exchange.respond {
                val body = "not here".toByteArray()
                it.sendResponseHeaders(404, body.size.toLong())
                it.responseBody.write(body)
            }
        }
        server.createContext("/slow") { exchange ->
            exchange.respond {
                it.sendResponseHeaders(200, content.size.toLong())
                releaseSlowResponse.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        }
        server.createContext("/drops") { exchange ->
            // the first request gets half of the file and a closed connection, the rest is served normally
            if (exchange.requestHeaders.getFirst("Range") == null) {
                exchange.responseHeaders.add("ETag", "\"v1\"")
                exchange.sendResponseHeaders(200, content.size.toLong())
                exchange.responseBody.write(content, 0, content.size / 2)
                exchange.responseBody.flush()
                exchange.close()
            } else {
                exchange.respond { serveFile(it) }
            }
        }
        server.start()
    }

    @AfterEach
    fun stopServer() {
        releaseSlowResponse.countDown()
        server.stop(0)
    }

    private fun serveFile(exchange: HttpExchange) {
        receivedHeaders += listOf("Range", "If-Range", "Accept-Encoding").map { "$it=${exchange.requestHeaders.getFirst(it)}" }
        exchange.responseHeaders.add("ETag", "\"v1\"")
        val range = exchange.requestHeaders.getFirst("Range")
        val ifRange = exchange.requestHeaders.getFirst("If-Range")
        if (range != null && (ifRange == null || ifRange == "\"v1\"")) {
            val from = range.removePrefix("bytes=").removeSuffix("-").toInt()
            exchange.responseHeaders.add("Content-Range", "bytes $from-${content.size - 1}/${content.size}")
            exchange.sendResponseHeaders(206, (content.size - from).toLong())
            exchange.responseBody.write(content, from, content.size - from)
        } else {
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.write(content)
        }
    }

    private fun url(path: String) = URI("http://${server.address.hostString}:${server.address.port}$path")

    private fun HttpExchange.respond(handle: (HttpExchange) -> Unit) {
        try {
            handle(this)
        } finally {
            close()
        }
    }

    private fun open(url: URI, offset: Long = 0, ifRange: String? = null, transport: HttpTransport = HttpTransport()) =
        transport.open(DownloadRequest(url, offset, ifRange))

    @Test
    fun `downloads the whole file`() {
        open(url("/file")).use { response ->
            assertEquals(200, response.status)
            assertEquals("1000", response.header("content-length"))
            assertEquals("\"v1\"", response.header("ETag"))
            assertContentEquals(content, response.body.readBytes())
        }
        assertEquals(listOf("Range=null", "If-Range=null", "Accept-Encoding=identity"), receivedHeaders)
    }

    @Test
    fun `asks for a range from the offset`() {
        open(url("/file"), offset = 600, ifRange = "\"v1\"").use { response ->
            assertEquals(206, response.status)
            assertEquals("bytes 600-999/1000", response.header("Content-Range"))
            assertContentEquals(content.copyOfRange(600, 1000), response.body.readBytes())
        }
        assertEquals(listOf("Range=bytes=600-", "If-Range=\"v1\"", "Accept-Encoding=identity"), receivedHeaders)
    }

    @Test
    fun `follows a relative redirect`() {
        open(url("/redirect")).use { response ->
            assertEquals(200, response.status)
            assertContentEquals(content, response.body.readBytes())
        }
    }

    @Test
    fun `stops after too many redirects`() {
        val exception = assertFailsWith<IOException> { open(url("/redirect-loop")) }
        assertEquals("Cannot download '${url("/redirect-loop")}': more than 5 redirects", exception.message)
    }

    @Test
    fun `returns an error status with its body`() {
        open(url("/missing")).use { response ->
            assertEquals(404, response.status)
            assertEquals("not here", String(response.body.readBytes()))
        }
    }

    @Test
    fun `gives up on a server that stops sending`() {
        val exception = assertFailsWith<SocketTimeoutException> {
            open(url("/slow"), transport = HttpTransport(readTimeoutMillis = 200)).use { it.body.readBytes() }
        }
        assertEquals("Read timed out", exception.message)
    }

    @Test
    fun `a downloader continues after the server closed the connection`() {
        val target = tempDir.resolve("tool.bin")
        val downloader = Downloader(
            offline = false, retry = RetryPolicy(), transport = HttpTransport(), listener = ProvisioningListener.None,
            sleep = {}, nanoTime = System::nanoTime,
        )

        downloader.download(RemoteFile(url("/drops")), target)

        assertContentEquals(content, Files.readAllBytes(target))
    }

    @Test
    fun `reads file URLs`() {
        val file = Files.write(tempDir.resolve("tool bin ü.tar.gz"), content)

        HttpTransport().open(DownloadRequest(file.toUri(), 0, null)).use { response ->
            assertEquals(200, response.status)
            assertEquals("1000", response.header("Content-Length"))
            assertContentEquals(content, response.body.readBytes())
        }
        val validator = Files.getLastModifiedTime(file).toMillis().toString()
        HttpTransport().open(DownloadRequest(file.toUri(), 250, validator)).use { response ->
            assertEquals(206, response.status)
            assertEquals("bytes 250-999/1000", response.header("Content-Range"))
            assertContentEquals(content.copyOfRange(250, 1000), response.body.readBytes())
        }
        HttpTransport().open(DownloadRequest(file.toUri(), 250, "something else")).use { response ->
            assertEquals(200, response.status, "a changed file is sent whole")
        }
        HttpTransport().open(DownloadRequest(tempDir.resolve("missing").toUri(), 0, null)).use { response ->
            assertEquals(404, response.status)
        }
    }

    @Test
    fun `rejects unsupported schemes`() {
        val exception = assertFailsWith<IllegalArgumentException> { open(URI("ftp://example.org/tool")) }
        assertEquals("Cannot download 'ftp://example.org/tool': only http, https and file URLs are supported", exception.message)
    }

    @Test
    fun `redirects resolve relative locations and never leave https`() {
        val from = URI("https://example.org/dist/tool.tar.gz")
        assertEquals(URI("https://example.org/mirror/tool.tar.gz"), redirectTarget(from, "/mirror/tool.tar.gz"))
        assertEquals(URI("https://cdn.example.org/tool.tar.gz"), redirectTarget(from, "https://cdn.example.org/tool.tar.gz"))
        assertEquals(URI("https://example.org/tool.tar.gz"), redirectTarget(URI("http://example.org/tool.tar.gz"), "https://example.org/tool.tar.gz"))

        val downgrade = assertFailsWith<IOException> { redirectTarget(from, "http://example.org/tool.tar.gz") }
        assertEquals(
            "Refusing to follow the redirect from '$from' to 'http://example.org/tool.tar.gz': it leaves HTTPS",
            downgrade.message,
        )
        assertFailsWith<IOException> { redirectTarget(from, "http://exa mple.org") }

        for (target in listOf("file:///etc/hosts", "ftp://example.org/tool.tar.gz", "jar:file:/tmp/a.jar!/tool")) {
            val plainHttp = URI("http://example.org/tool.tar.gz")
            assertEquals(
                "Refusing to follow the redirect from '$plainHttp' to '$target': only http and https URLs are followed",
                assertFailsWith<IOException>(target) { redirectTarget(plainHttp, target) }.message,
            )
        }
    }

    @Test
    fun `header names are compared without regard to case`() {
        val response = DownloadResponse(200, mapOf("ETag" to "x"), "".byteInputStream())
        assertEquals("x", response.header("etag"))
        assertNull(response.header("Last-Modified"))
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}
