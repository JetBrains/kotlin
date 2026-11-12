/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DistributionProvisionerTest {

    @TempDir
    lateinit var tempDir: Path

    private val storeDir: Path get() = tempDir.resolve("store")
    private val store by lazy { InstallationStore(storeDir) }

    private val id = "tool-1.0"

    private fun toolZip(name: String = "tool-1.0.zip") =
        writeZip(tempDir.resolve(name), "tool-1.0/bin/tool" to "binary", "tool-1.0/README" to "readme")

    @Test
    fun `installs a local archive`() {
        val archive = toolZip()

        val installation = DistributionProvisioner(store).provision(Distribution(id, ArchiveSource.Local(archive), Archives.zip()))

        assertEquals(storeDir.resolve(id), installation)
        assertEquals("binary", installation.resolve("bin/tool").readText())
        assertEquals(installation, store.find(id))
        assertTrue(Files.exists(archive), "a local archive is left alone")
    }

    @Test
    fun `downloads and installs a remote archive`() {
        val archive = toolZip()
        val distribution = Distribution(id, ArchiveSource.Remote(RemoteFile(archive.toUri())), Archives.zip())

        val installation = DistributionProvisioner(store, Downloader(transport = HttpTransport())).provision(distribution)

        assertEquals("readme", installation.resolve("README").readText())
        assertFalse(Files.exists(storeDir.resolve(".downloads/$id")), "the download is deleted once installed")
    }

    @Test
    fun `takes the whole archive without a root directory`() {
        val archive = writeZip(tempDir.resolve("flat.zip"), "bin/tool" to "binary")

        val installation = DistributionProvisioner(store)
            .provision(Distribution(id, ArchiveSource.Local(archive), Archives.zip(), rootDirectory = null))

        assertEquals("binary", installation.resolve("bin/tool").readText())
    }

    @Test
    fun `takes a nested root directory`() {
        val archive = writeZip(tempDir.resolve("nested.zip"), "package/tool-1.0/bin/tool" to "binary", "package/other" to "x")

        val installation = DistributionProvisioner(store)
            .provision(Distribution(id, ArchiveSource.Local(archive), Archives.zip(), rootDirectory = "package/tool-1.0"))

        assertEquals("binary", installation.resolve("bin/tool").readText())
        assertFalse(Files.exists(installation.resolve("other")))
    }

    @Test
    fun `an archive without the root directory installs nothing`() {
        val archive = writeZip(tempDir.resolve("other.zip"), "something-else/tool" to "x", "LICENSE" to "x")

        val exception = assertFailsWith<InvalidDistributionException> {
            DistributionProvisioner(store).provision(Distribution(id, ArchiveSource.Local(archive), Archives.zip()))
        }

        assertEquals(
            "The archive '$archive' of '$id' has no directory '$id'. Its top level contains: LICENSE, something-else.",
            exception.message,
        )
        assertNull(store.find(id))
    }

    @Test
    fun `root directories outside the archive are rejected`() {
        for (root in listOf("", "..", "a/../..", tempDir.toAbsolutePath().toString())) {
            val exception = assertFailsWith<IllegalArgumentException>("'$root'") {
                Distribution(id, ArchiveSource.Local(toolZip()), Archives.zip(), rootDirectory = root)
            }
            assertEquals("The root directory of a distribution must be a relative path inside the archive, got '$root'", exception.message)
        }
    }

    @Test
    fun `set-up runs before the installation is published`() {
        var seenAt: Path? = null

        val installation = DistributionProvisioner(store).provision(
            Distribution(id, ArchiveSource.Local(toolZip()), Archives.zip(), setUp = { root ->
                seenAt = root
                assertNull(store.find(id), "nobody sees the installation yet")
                Files.write(root.resolve("configured"), byteArrayOf(1))
            })
        )

        assertTrue(Files.exists(installation.resolve("configured")))
        assertTrue(seenAt!!.startsWith(storeDir.resolve(".staging")))
    }

    @Test
    fun `failures of the extractor and the set-up pass through and install nothing`() {
        val extractorFailure = IllegalStateException("cannot unpack")
        val setUpFailure = IllegalStateException("cannot set up")
        val provisioner = DistributionProvisioner(store)

        assertSame(extractorFailure, assertFailsWith<IllegalStateException> {
            provisioner.provision(Distribution(id, ArchiveSource.Local(toolZip()), { _, _ -> throw extractorFailure }))
        })
        assertSame(setUpFailure, assertFailsWith<IllegalStateException> {
            provisioner.provision(Distribution(id, ArchiveSource.Local(toolZip()), Archives.zip(), setUp = { throw setUpFailure }))
        })
        assertNull(store.find(id))
    }

    @Test
    fun `a missing local archive fails`() {
        val missing = tempDir.resolve("missing.zip")

        val exception = assertFailsWith<NoSuchFileException> {
            DistributionProvisioner(store).provision(Distribution(id, ArchiveSource.Local(missing), Archives.zip()))
        }

        assertEquals("$missing: the archive of '$id' does not exist", exception.message)
    }

    @Test
    fun `an installed distribution is not unpacked again`() {
        val extractions = AtomicInteger()
        val countingZip = ArchiveExtractor { archive, destination ->
            extractions.incrementAndGet()
            Archives.zip().extract(archive, destination)
        }
        val provisioner = DistributionProvisioner(store)
        val distribution = Distribution(id, ArchiveSource.Local(toolZip()), countingZip)

        assertEquals(provisioner.provision(distribution), provisioner.provision(distribution))
        assertEquals(1, extractions.get())
    }

    @Test
    fun `reports unpacking to its listener`() {
        val events = mutableListOf<String>()
        val listener = object : ProvisioningListener {
            override fun extractionStarted(archive: Path, destination: Path) {
                events += "${archive.fileName} into ${destination.fileName}"
            }
        }

        DistributionProvisioner(store, listener = listener).provision(Distribution(id, ArchiveSource.Local(toolZip()), Archives.zip()))

        assertEquals(listOf("tool-1.0.zip into unpacked"), events)
    }

    @Test
    fun `a failed download is continued by the next attempt`() {
        val bytes = Files.readAllBytes(toolZip())
        val url = URI("https://example.org/dist/tool-1.0.zip")
        val requests = mutableListOf<Long>()
        val answers = ArrayDeque(
            listOf<(DownloadRequest) -> DownloadResponse>(
                { DownloadResponse(200, mapOf("Content-Length" to "${bytes.size}", "ETag" to "\"v1\""), BrokenStream(bytes.copyOf(20))) },
                {
                    DownloadResponse(
                        206,
                        mapOf("Content-Range" to "bytes 20-${bytes.size - 1}/${bytes.size}"),
                        ByteArrayInputStream(bytes, 20, bytes.size - 20),
                    )
                },
            )
        )
        val transport = DownloadTransport { request -> requests += request.offset; answers.removeFirst()(request) }
        val downloader = Downloader(
            offline = false, retry = RetryPolicy(maxAttempts = 1), transport = transport, listener = ProvisioningListener.None,
            sleep = {}, nanoTime = System::nanoTime,
        )
        val provisioner = DistributionProvisioner(store, downloader)
        val distribution = Distribution(id, ArchiveSource.Remote(RemoteFile(url)), Archives.zip())

        assertFailsWith<DownloadFailedException> { provisioner.provision(distribution) }
        assertTrue(Files.exists(storeDir.resolve(".downloads/$id/tool-1.0.zip.part")), "the part file survives for the next build")

        val installation = provisioner.provision(distribution)

        assertEquals(listOf(0L, 20L), requests)
        assertEquals("binary", installation.resolve("bin/tool").readText())
        assertFalse(Files.exists(storeDir.resolve(".downloads/$id")), "the download is deleted once installed")
    }

    @Test
    fun `an archive that was downloaded completely is not downloaded again when unpacking fails`() {
        val bytes = Files.readAllBytes(toolZip())
        val sha256 = sha256Of(tempDir.resolve("tool-1.0.zip"))
        val requests = AtomicInteger()
        val transport = DownloadTransport {
            requests.incrementAndGet()
            DownloadResponse(200, mapOf("Content-Length" to "${bytes.size}"), ByteArrayInputStream(bytes))
        }
        val provisioner = DistributionProvisioner(store, Downloader(transport = transport))
        val archive = ArchiveSource.Remote(RemoteFile(URI("https://example.org/dist/tool-1.0.zip"), sha256))
        val unpackingFailure = IllegalStateException("cannot unpack")

        assertSame(unpackingFailure, assertFailsWith<IllegalStateException> {
            provisioner.provision(Distribution(id, archive, { _, _ -> throw unpackingFailure }))
        })
        val installation = provisioner.provision(Distribution(id, archive, Archives.zip()))

        assertEquals(1, requests.get())
        assertEquals("binary", installation.resolve("bin/tool").readText())
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "creating symlinks needs a privilege on Windows")
    fun `set-up does not run on a root directory that is a link out of the archive`() {
        val outside = Files.createDirectories(tempDir.resolve("outside"))
        // an extractor that restores symlinks, as tar extractors do
        val linkingExtractor = ArchiveExtractor { _, destination -> Files.createSymbolicLink(destination.resolve(id), outside) }
        var setUpRan = false

        val exception = assertFailsWith<InvalidDistributionException> {
            DistributionProvisioner(store)
                .provision(Distribution(id, ArchiveSource.Local(toolZip()), linkingExtractor, setUp = { setUpRan = true }))
        }

        assertEquals(
            "The directory '$id' in the archive '${tempDir.resolve("tool-1.0.zip")}' of '$id' is a link that leads outside the archive.",
            exception.message,
        )
        assertFalse(setUpRan)
    }

    @Test
    fun `a file name from the URL that is not a plain name is not used`() {
        val transport = DownloadTransport { DownloadResponse(200, mapOf("Content-Length" to "10", "ETag" to "x"), BrokenStream(ByteArray(3))) }
        val downloader = Downloader(false, RetryPolicy(maxAttempts = 1), transport, ProvisioningListener.None, sleep = {}, nanoTime = { 0 })
        val provisioner = DistributionProvisioner(store, downloader)

        // '%5C' decodes to a backslash, which is a path separator on Windows
        for (url in listOf("https://example.org/dist/..%5C..%5C..%5Cx.zip", "https://example.org/dist/", "https://example.org")) {
            assertFailsWith<DownloadFailedException> {
                provisioner.provision(Distribution(id, ArchiveSource.Remote(RemoteFile(URI(url))), Archives.zip()))
            }
            assertEquals(
                listOf("archive.part", "archive.part.validator"),
                Files.list(storeDir.resolve(".downloads/$id")).use { files -> files.iterator().asSequence().map { it.fileName.toString() }.sorted().toList() },
                url,
            )
        }
    }

    private class BrokenStream(bytes: ByteArray) : InputStream() {
        private val data = ByteArrayInputStream(bytes)
        override fun read(): Int = data.read().also { if (it < 0) throw SocketTimeoutException("Read timed out") }
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            data.read(b, off, len).also { if (it < 0) throw SocketTimeoutException("Read timed out") }
    }
}
