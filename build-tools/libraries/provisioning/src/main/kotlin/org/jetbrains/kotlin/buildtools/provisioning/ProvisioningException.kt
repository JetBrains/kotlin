/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
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
