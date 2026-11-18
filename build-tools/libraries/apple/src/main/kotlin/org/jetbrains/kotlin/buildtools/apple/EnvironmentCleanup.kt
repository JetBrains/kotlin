/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

/**
 * Variables to remove from the environment a tool inherits.
 *
 * When a build runs from an Xcode build phase, Xcode exports its build settings as variables, and some of them change
 * what a nested `xcodebuild` or `swift` does. The presets name the sets that matter; combine them with [plus].
 *
 * @property names variables removed by exact name.
 * @property prefixes every variable starting with one of these is removed.
 */
public class EnvironmentCleanup(
    public val names: Set<String> = emptySet(),
    public val prefixes: Set<String> = emptySet(),
) {
    /** Whether the variable called [name] is removed. */
    public fun removes(name: String): Boolean = name in names || prefixes.any { name.startsWith(it) }

    /** Removes what either cleanup removes. */
    public operator fun plus(other: EnvironmentCleanup): EnvironmentCleanup =
        EnvironmentCleanup(names + other.names, prefixes + other.prefixes)

    override fun equals(other: Any?): Boolean = other is EnvironmentCleanup && names == other.names && prefixes == other.prefixes

    override fun hashCode(): Int = 31 * names.hashCode() + prefixes.hashCode()

    override fun toString(): String = "EnvironmentCleanup(names=$names, prefixes=$prefixes)"

    public companion object {
        /** Removes nothing. */
        @JvmField
        public val NONE: EnvironmentCleanup = EnvironmentCleanup()

        /** `SDKROOT`, which selects one SDK for every tool and so breaks builds for any other platform. */
        @JvmField
        public val SDKROOT: EnvironmentCleanup = EnvironmentCleanup(names = setOf("SDKROOT"))

        /** Every `SDK*` variable. With them set, SwiftPM compiles package manifests for the app's platform. */
        @JvmField
        public val SDK_VARIABLES: EnvironmentCleanup = EnvironmentCleanup(prefixes = setOf("SDK"))

        /** Variables that make `xcodebuild` link the debug dylib of the surrounding app. */
        @JvmField
        public val DEBUG_DYLIB: EnvironmentCleanup = EnvironmentCleanup(
            names = setOf(
                "ENABLE_DEBUG_DYLIB",
                "EXECUTABLE_BLANK_INJECTION_DYLIB_PATH",
                "EXECUTABLE_DEBUG_DYLIB_INSTALL_NAME",
                "EXECUTABLE_DEBUG_DYLIB_PATH",
            )
        )

        /**
         * Everything that ties a nested `xcodebuild` to the app that started the build: [DEBUG_DYLIB],
         * `EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES`, and the per-target `OTHER_*` and `ASSETCATALOG_*` build settings.
         */
        @JvmField
        public val APP_BUILD_SETTINGS: EnvironmentCleanup = DEBUG_DYLIB + EnvironmentCleanup(
            names = setOf("EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES"),
            prefixes = setOf("OTHER_", "ASSETCATALOG_"),
        )
    }
}
