/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/** Which Xcode [XcodeLocator.resolve] should return. */
public sealed interface XcodeRequest {
    /** The Xcode `DEVELOPER_DIR` names, or the `xcode-select` choice when it is not set. */
    public data object Selected : XcodeRequest

    /** The installation at [path], which is an `.app` bundle, its `Contents/Developer`, or a Command Line Tools root. */
    public data class DeveloperDir(public val path: Path) : XcodeRequest

    /**
     * An installed Xcode matching [spec].
     *
     * The spec is either a version prefix (`16`, `16.4`, `16.4.1`), which picks the highest matching version, or a build
     * number such as `16F6`, which must match exactly. Anything else throws [IllegalArgumentException].
     */
    public data class Version(public val spec: String) : XcodeRequest {
        init {
            require(isBuildNumber || versionPrefixPattern.matches(spec)) {
                "'$spec' is neither an Xcode version such as 16.4 nor a build number such as 16F6"
            }
        }

        internal val isBuildNumber: Boolean get() = buildNumberPattern.matches(spec)

        /** The components of a version prefix: `[16, 4]` for `16.4`. */
        internal val versionComponents: List<String> get() = spec.split('.')

        private companion object {
            val versionPrefixPattern = Regex("""\d+(\.\d+){0,2}""")
            val buildNumberPattern = Regex("""\d+[A-Z]\d+[a-z]?""")
        }
    }
}
