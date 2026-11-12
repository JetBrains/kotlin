/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A tool distribution to install: where its archive comes from and how to turn it into an installation.
 *
 * @property id the installation id in the store, for example `node-v24.22.1-linux-x64`.
 * @property archive where the archive comes from.
 * @property extractor unpacks the archive.
 * @property rootDirectory the directory in the archive that becomes the installation, as a relative path. Archives usually
 * keep everything in one top-level directory named like the distribution, hence the default. Null takes the whole archive.
 * @property setUp runs on the unpacked installation before anyone can see it, for example to fix symlinks or executable
 * bits. If it throws, nothing is installed.
 */
public class Distribution(
    public val id: String,
    public val archive: ArchiveSource,
    public val extractor: ArchiveExtractor,
    public val rootDirectory: String? = id,
    public val setUp: (installation: Path) -> Unit = {},
) {
    init {
        if (rootDirectory != null) {
            val path = Paths.get(rootDirectory)
            // 'root', not 'isAbsolute': on Windows '\dir' has a root without being absolute, and resolves to the drive
            require(rootDirectory.isNotBlank() && path.root == null && path.normalize().none { it.toString() == ".." }) {
                "The root directory of a distribution must be a relative path inside the archive, got '$rootDirectory'"
            }
        }
    }
}

/** Where the archive of a [Distribution] comes from. */
public sealed interface ArchiveSource {
    /**
     * An archive to download.
     *
     * @property file where to download it from.
     */
    public class Remote(public val file: RemoteFile) : ArchiveSource

    /**
     * An archive that is already on disk, for example one resolved by the build tool. It is read, not moved or deleted.
     *
     * @property archive the archive file.
     */
    public class Local(public val archive: Path) : ArchiveSource
}

/**
 * Installs distributions into a store: downloads the archive if needed, unpacks it, runs the distribution's set-up and
 * publishes the result. Concurrent requests for one distribution do the work once, see [InstallationStore.install].
 *
 * A download is kept in the store between attempts, in `.downloads/<id>`, so that a download that failed is continued by the
 * next build, and an archive with a checksum that was downloaded completely is not downloaded again when unpacking fails. It
 * is deleted once the distribution is installed.
 *
 * @param store where installations go.
 * @param downloader downloads remote archives.
 * @param listener receives unpacking events. Store and download events go to the listeners of [store] and [downloader].
 */
public class DistributionProvisioner(
    private val store: InstallationStore,
    private val downloader: Downloader = Downloader(),
    private val listener: ProvisioningListener = ProvisioningListener.None,
) {
    /**
     * Returns the installation of [distribution], installing it first if there is none.
     *
     * Exceptions of the extractor and of [Distribution.setUp] reach the caller unchanged, and so do those of the [Downloader]:
     * [OfflineException], [DownloadFailedException] and [ChecksumMismatchException].
     *
     * @throws NoSuchFileException if a [ArchiveSource.Local] archive does not exist.
     * @throws InvalidDistributionException if the archive has no [Distribution.rootDirectory].
     */
    public fun provision(distribution: Distribution): Path {
        var downloads: Path? = null
        val installation = store.install(distribution.id) { staging ->
            val archive = when (val source = distribution.archive) {
                is ArchiveSource.Local -> source.archive.also {
                    if (!Files.isRegularFile(it)) {
                        throw NoSuchFileException(it.toString(), null, "the archive of '${distribution.id}' does not exist")
                    }
                }
                is ArchiveSource.Remote -> {
                    val directory = store.downloadDirectory(distribution.id).also { downloads = it }
                    val target = directory.resolve(archiveFileName(source.file))
                    val sha256 = source.file.sha256
                    // an earlier attempt may have got the whole archive and then failed to unpack it
                    if (sha256 != null && Files.isRegularFile(target) && sha256Of(target) == sha256) {
                        target
                    } else {
                        downloader.download(source.file, target)
                    }
                }
            }

            val unpacked = Files.createDirectory(staging.directory.resolve("unpacked"))
            listener.extractionStarted(archive, unpacked)
            distribution.extractor.extract(archive, unpacked)

            val root = distribution.rootDirectory?.let { unpacked.resolve(it).normalize() } ?: unpacked
            if (!Files.isDirectory(root)) {
                val topLevel = Files.list(unpacked).use { entries ->
                    entries.iterator().asSequence().map { it.fileName.toString() }.sorted().toList()
                }
                throw InvalidDistributionException(
                    "The archive '$archive' of '${distribution.id}' has no directory '${distribution.rootDirectory}'. " +
                            "Its top level contains: ${topLevel.joinToString().ifEmpty { "nothing" }}."
                )
            }
            // Checked before the set-up runs on it: an extractor that restores symlinks could have made the root one
            // that leads out of the archive.
            if (!root.toRealPath().startsWith(unpacked.toRealPath())) {
                throw InvalidDistributionException(
                    "The directory '${distribution.rootDirectory}' in the archive '$archive' of '${distribution.id}' is a link " +
                            "that leads outside the archive."
                )
            }
            distribution.setUp(root)
            root
        }

        // The archive goes only after the installation is published, and a failure to delete it does not undo that.
        downloads?.let {
            try {
                deleteRecursively(it)
            } catch (e: IOException) {
                // a stray archive wastes some disk space and nothing else
            }
        }
        return installation
    }

    /**
     * The name to keep the download under. Extractors may go by the extension, so the name from the URL is kept when it
     * is a plain file name. A URL is outside input, though: anything else, a name with `..\` in it for example, is not used.
     */
    private fun archiveFileName(file: RemoteFile): String {
        val name = file.urls.first().path?.substringAfterLast('/').orEmpty()
        return if (isSingleFileName(name)) name else "archive"
    }
}
