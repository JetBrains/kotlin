/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Locale
import java.util.TreeMap

private const val MAX_REDIRECTS = 5
private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)

/**
 * Opens a URL for [Downloader]. Implement it to download with another HTTP client, or from something that is not an HTTP
 * server.
 */
public fun interface DownloadTransport {
    /**
     * Sends [request] and returns the response. An HTTP error status is a response, not an exception. A failure to connect
     * or to read throws [IOException].
     */
    public fun open(request: DownloadRequest): DownloadResponse
}

/**
 * One request of [Downloader].
 *
 * @property url what to download.
 * @property offset when greater than zero, only the bytes from this offset on are wanted: an HTTP `Range` request.
 * @property ifRange the validator (`ETag` or `Last-Modified`) of the earlier response the offset refers to. A server sends the
 * range only while the file still has this validator, and the whole file otherwise: an HTTP `If-Range` request.
 */
public class DownloadRequest internal constructor(
    public val url: URI,
    public val offset: Long,
    public val ifRange: String?,
)

/**
 * The answer to a [DownloadRequest]. Closing it closes [body] and releases the connection.
 *
 * @property status the HTTP status code. Transports for other sources use the HTTP codes with the same meaning: 200 for the
 * whole file, 206 for a range, 404 for a missing file.
 * @property body the response body.
 * @param headers the response headers. Of a header that occurs several times, one value is enough.
 * @param onClose called after [body] is closed, to release what the transport holds for this response.
 */
public class DownloadResponse(
    public val status: Int,
    headers: Map<String, String>,
    public val body: InputStream,
    private val onClose: () -> Unit = {},
) : Closeable {
    private val headers = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).apply { putAll(headers) }

    /** The value of the header [name], compared without regard to case, or null. */
    public fun header(name: String): String? = headers[name]

    override fun close() {
        try {
            body.close()
        } finally {
            onClose()
        }
    }
}

/**
 * The default [DownloadTransport]. Downloads `http` and `https` URLs with [HttpURLConnection], using the JVM's proxy and
 * TLS settings, and reads `file` URLs from disk.
 *
 * Redirects are followed, at most five in a row, and only to `http` and `https` URLs. A redirect never leaves `https`.
 *
 * @param connectTimeoutMillis how long to wait for a connection.
 * @param readTimeoutMillis how long to wait for the next bytes once connected.
 */
public class HttpTransport(
    private val connectTimeoutMillis: Int = 30_000,
    private val readTimeoutMillis: Int = 60_000,
) : DownloadTransport {

    /** @throws IllegalArgumentException for a URL scheme other than `http`, `https` and `file`. */
    override fun open(request: DownloadRequest): DownloadResponse = when (request.url.scheme?.lowercase(Locale.ROOT)) {
        "http", "https" -> openHttp(request)
        "file" -> openFile(request)
        else -> throw IllegalArgumentException("Cannot download '${request.url}': only http, https and file URLs are supported")
    }

    private fun openHttp(request: DownloadRequest): DownloadResponse {
        var url = request.url
        repeat(MAX_REDIRECTS + 1) {
            val connection = url.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = connectTimeoutMillis
                connection.readTimeout = readTimeoutMillis
                // a compressed body would not match Content-Length, offsets or the checksum
                connection.setRequestProperty("Accept-Encoding", "identity")
                if (request.offset > 0) {
                    connection.setRequestProperty("Range", "bytes=${request.offset}-")
                    request.ifRange?.let { connection.setRequestProperty("If-Range", it) }
                }
                val status = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (status in REDIRECT_STATUSES && location != null) {
                    connection.disconnect()
                    url = redirectTarget(url, location)
                    return@repeat
                }
                // the status line comes with a null name
                val headers = connection.headerFields.entries
                    .filter { it.key != null && it.value.isNotEmpty() }
                    .associate { it.key to it.value.last() }
                val body = (if (status >= 400) connection.errorStream else connection.inputStream) ?: emptyStream()
                return DownloadResponse(status, headers, body) { connection.disconnect() }
            } catch (e: IOException) {
                connection.disconnect()
                throw e
            }
        }
        throw IOException("Cannot download '${request.url}': more than $MAX_REDIRECTS redirects")
    }

    private fun openFile(request: DownloadRequest): DownloadResponse {
        val path = Paths.get(request.url)
        if (!Files.isRegularFile(path)) return DownloadResponse(404, emptyMap(), emptyStream())

        val size = Files.size(path)
        val validator = Files.getLastModifiedTime(path).toMillis().toString()
        val headers = mutableMapOf("Last-Modified" to validator)
        val channel = Files.newByteChannel(path)
        val offset = request.offset
        return if (offset in 1 until size && (request.ifRange == null || request.ifRange == validator)) {
            channel.position(offset)
            headers["Content-Range"] = "bytes $offset-${size - 1}/$size"
            headers["Content-Length"] = (size - offset).toString()
            DownloadResponse(206, headers, Channels.newInputStream(channel))
        } else {
            headers["Content-Length"] = size.toString()
            DownloadResponse(200, headers, Channels.newInputStream(channel))
        }
    }
}

/** Where a redirect from [from] to [location] leads. */
internal fun redirectTarget(from: URI, location: String): URI {
    val target = try {
        from.resolve(location.trim())
    } catch (e: IllegalArgumentException) {
        throw IOException("Cannot follow the redirect from '$from': invalid location '$location'", e)
    }
    if (from.scheme.equals("https", ignoreCase = true) && !target.scheme.equals("https", ignoreCase = true)) {
        throw IOException("Refusing to follow the redirect from '$from' to '$target': it leaves HTTPS")
    }
    // a server must not be able to point the download at a local file or at another protocol
    if (!target.scheme.equals("http", ignoreCase = true) && !target.scheme.equals("https", ignoreCase = true)) {
        throw IOException("Refusing to follow the redirect from '$from' to '$target': only http and https URLs are followed")
    }
    return target
}

private fun emptyStream(): InputStream = ByteArrayInputStream(ByteArray(0))
