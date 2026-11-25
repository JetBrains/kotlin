/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/** `/usr/bin/xattr`. */
public class Xattr internal constructor(private val tools: AppleTools) {

    /**
     * `xattr -w com.apple.xcode.CreatedByBuildSystem true <directory>`.
     *
     * Xcode's "Clean Build Folder" only deletes directories that carry this attribute.
     */
    public fun markCreatedByBuildSystemInvocation(directory: Path): AppleToolInvocation =
        tools.systemInvocation(XATTR, listOf("-w", CREATED_BY_BUILD_SYSTEM, "true", directory.toString()))

    /** Runs [markCreatedByBuildSystemInvocation]. */
    public fun markCreatedByBuildSystem(directory: Path) {
        tools.runner.runChecked(markCreatedByBuildSystemInvocation(directory))
    }

    private companion object {
        val XATTR: Path = Paths.get("/usr/bin/xattr")
        const val CREATED_BY_BUILD_SYSTEM = "com.apple.xcode.CreatedByBuildSystem"
    }
}
