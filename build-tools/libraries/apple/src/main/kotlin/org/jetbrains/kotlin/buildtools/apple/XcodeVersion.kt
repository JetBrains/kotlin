/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

/**
 * An Xcode marketing version such as `16.4` or `16.4.1`. A missing component is `0`, so `16` equals `16.0.0`.
 *
 * [toString] omits a zero [patch]: `16.4`, `16.4.1`.
 */
public data class XcodeVersion(
    /** The major version, `16` in `16.4.1`. */
    public val major: Int,
    /** The minor version, `4` in `16.4.1`. */
    public val minor: Int = 0,
    /** The patch version, `1` in `16.4.1`. */
    public val patch: Int = 0,
) : Comparable<XcodeVersion> {

    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Version components must not be negative: $major.$minor.$patch" }
    }

    override fun compareTo(other: XcodeVersion): Int =
        compareValuesBy(this, other, XcodeVersion::major, XcodeVersion::minor, XcodeVersion::patch)

    override fun toString(): String = if (patch == 0) "$major.$minor" else "$major.$minor.$patch"

    public companion object {
        private val versionPattern = Regex("""(\d+)(?:\.(\d+))?(?:\.(\d+))?""")

        /** Parses `16`, `16.4` or `16.4.1`, with surrounding whitespace allowed. Returns `null` for anything else. */
        @JvmStatic
        public fun parse(text: String): XcodeVersion? {
            val match = versionPattern.matchEntire(text.trim()) ?: return null
            val (major, minor, patch) = match.destructured
            return XcodeVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.ifEmpty { "0" }.toIntOrNull() ?: return null,
                patch = patch.ifEmpty { "0" }.toIntOrNull() ?: return null,
            )
        }
    }
}
