/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

/**
 * An Apple SDK platform, with the names different tools use for it.
 *
 * @property sdkName the SDK name for `xcrun --sdk` and `xcodebuild -sdk`, such as `iphonesimulator`.
 * @property destinationPlatform the platform in an `xcodebuild -destination`, such as `iOS Simulator`.
 * @property bundlePlatformName the value for `CFBundleSupportedPlatforms` in `Info.plist`, such as `iPhoneSimulator`.
 * @property isSimulator whether binaries for this platform run in a simulator.
 */
public enum class ApplePlatform(
    public val sdkName: String,
    public val destinationPlatform: String,
    public val bundlePlatformName: String,
    public val isSimulator: Boolean,
) {
    /** macOS. */
    MACOS("macosx", "macOS", "MacOSX", isSimulator = false),

    /** iOS devices. */
    IOS("iphoneos", "iOS", "iPhoneOS", isSimulator = false),

    /** The iOS simulator. */
    IOS_SIMULATOR("iphonesimulator", "iOS Simulator", "iPhoneSimulator", isSimulator = true),

    /** tvOS devices. */
    TVOS("appletvos", "tvOS", "AppleTVOS", isSimulator = false),

    /** The tvOS simulator. */
    TVOS_SIMULATOR("appletvsimulator", "tvOS Simulator", "AppleTVSimulator", isSimulator = true),

    /** watchOS devices. */
    WATCHOS("watchos", "watchOS", "WatchOS", isSimulator = false),

    /** The watchOS simulator. */
    WATCHOS_SIMULATOR("watchsimulator", "watchOS Simulator", "WatchSimulator", isSimulator = true),

    /** visionOS devices. */
    VISIONOS("xros", "visionOS", "XROS", isSimulator = false),

    /** The visionOS simulator. */
    VISIONOS_SIMULATOR("xrsimulator", "visionOS Simulator", "XRSimulator", isSimulator = true),
    ;

    /** The `xcodebuild -destination` value for any device of this platform: `generic/platform=<destinationPlatform>`. */
    public val genericDestination: String get() = "generic/platform=$destinationPlatform"

    public companion object {
        /** The platform whose [sdkName] is [sdkName], or `null`. */
        @JvmStatic
        public fun fromSdkName(sdkName: String): ApplePlatform? = entries.firstOrNull { it.sdkName == sdkName }
    }
}
