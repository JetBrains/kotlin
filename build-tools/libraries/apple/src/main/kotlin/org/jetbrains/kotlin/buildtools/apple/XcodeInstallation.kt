/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/** Whether a developer dir belongs to Xcode.app or to the standalone Command Line Tools. */
public enum class XcodeKind {
    /** A full Xcode.app bundle. */
    XCODE,

    /** Command Line Tools, or any developer dir outside an `.app` bundle. There is no `xcodebuild` or `simctl`. */
    COMMAND_LINE_TOOLS,
}

/**
 * One Xcode or Command Line Tools installation.
 *
 * Every tool run through [AppleTools] gets `DEVELOPER_DIR` set to [developerDir].
 */
public data class XcodeInstallation(
    /** `<bundle>/Contents/Developer` for Xcode, the installation root for Command Line Tools. */
    public val developerDir: Path,
    /** The `.app` bundle, or `null` for Command Line Tools. */
    public val appBundle: Path?,
    /** Xcode or Command Line Tools. */
    public val kind: XcodeKind,
    /** `CFBundleShortVersionString` from the bundle's `version.plist`, or `null` for Command Line Tools. */
    public val version: XcodeVersion?,
    /** `ProductBuildVersion` from the bundle's `version.plist`, such as `16F6`, or `null` for Command Line Tools. */
    public val buildVersion: String?,
) {
    override fun toString(): String = when (kind) {
        XcodeKind.XCODE -> "Xcode ${version ?: "(unknown version)"}${buildVersion?.let { " ($it)" }.orEmpty()} at $appBundle"
        XcodeKind.COMMAND_LINE_TOOLS -> "Command Line Tools at $developerDir"
    }
}
