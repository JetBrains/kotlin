/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.net.URI
import java.nio.file.Path

/** A failure reported by this library. Failures of code the caller passes in reach the caller unchanged. */
public abstract class ProvisioningException internal constructor(message: String, cause: Throwable? = null) :
    IOException(message, cause)

/**
 * The code that was supposed to produce an installation produced something unusable, for example a path outside its
 * staging directory.
 */
public class InvalidDistributionException internal constructor(message: String) : ProvisioningException(message)

/**
 * A directory with an installation's name but without the marker file is in the way, and the store is set to
 * [LeftoverPolicy.FAIL].
 *
 * @property installation the directory in the way.
 */
public class LeftoverInstallationException internal constructor(
    public val installation: Path,
    markerFileName: String,
) : ProvisioningException(
    "'$installation' is not a complete installation: it has no '$markerFileName' file. It may be left over from an " +
            "interrupted installation or written by another installer. Delete it and run the build again."
)

/**
 * A file has to be downloaded, but the [Downloader] is offline.
 *
 * @property urls where the file would have come from.
 */
public class OfflineException internal constructor(
    public val urls: List<URI>,
    target: Path,
) : ProvisioningException(
    "Cannot download '${target.fileName}' from ${urls.first()}: downloading is disabled in offline mode. " +
            "Run the build once without offline mode."
)

/**
 * Every attempt to download a file failed.
 *
 * @property urls the URLs that were tried.
 */
public class DownloadFailedException internal constructor(
    public val urls: List<URI>,
    target: Path,
    cause: IOException,
) : ProvisioningException(
    "Cannot download '${target.fileName}' from ${urls.joinToString()}: ${cause.message}. Check the network connection, " +
            "proxy settings and the URL.",
    cause,
)

/**
 * The downloaded file does not have the expected SHA-256. The file on the server and the checksum the caller expects
 * disagree, so trying again does not help.
 *
 * @property url where the file came from.
 * @property expected the checksum the caller expected.
 * @property actual the checksum of the downloaded bytes.
 */
public class ChecksumMismatchException internal constructor(
    public val url: URI,
    public val expected: String,
    public val actual: String,
) : ProvisioningException(
    "The file downloaded from $url has the SHA-256 checksum $actual, but $expected was expected. Either the file on the " +
            "server or the expected checksum is wrong."
)
