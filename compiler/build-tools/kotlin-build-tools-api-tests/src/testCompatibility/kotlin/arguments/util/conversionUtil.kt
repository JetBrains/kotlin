/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.util

import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentConfiguration
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion
import org.junit.jupiter.api.Assumptions.assumeTrue

internal fun ArgumentConfiguration<*>.assumeArgumentAvailable() {
    assumeArgumentAvailable(
        KotlinToolingVersion(kotlinToolchain.getCompilerVersion()),
        KotlinToolingVersion(introducedVersion),
        removedVersion?.let { KotlinToolingVersion(it).toKotlinVersion().toKotlinToolingVersion("snapshot") })
}

private fun assumeArgumentAvailable(
    compilerVersion: KotlinToolingVersion,
    introducedVersion: KotlinToolingVersion,
    removedVersion: KotlinToolingVersion?,
) {
    assumeTrue(
        compilerVersion >= introducedVersion,
        "Test requires compiler version >= $introducedVersion"
    )

    if (removedVersion != null) {
        assumeTrue(
            compilerVersion < removedVersion,
            "Test requires compiler version < $removedVersion"
        )
    }
}
