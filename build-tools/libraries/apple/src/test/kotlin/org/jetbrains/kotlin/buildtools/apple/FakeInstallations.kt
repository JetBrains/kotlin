/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Paths

val fakeXcode: XcodeInstallation = XcodeInstallation(
    developerDir = Paths.get("/Applications/Xcode 16.app/Contents/Developer"),
    appBundle = Paths.get("/Applications/Xcode 16.app"),
    kind = XcodeKind.XCODE,
    version = XcodeVersion(16, 4),
    buildVersion = "16F6",
)

val fakeCommandLineTools: XcodeInstallation = XcodeInstallation(
    developerDir = Paths.get("/Library/Developer/CommandLineTools"),
    appBundle = null,
    kind = XcodeKind.COMMAND_LINE_TOOLS,
    version = null,
    buildVersion = null,
)
