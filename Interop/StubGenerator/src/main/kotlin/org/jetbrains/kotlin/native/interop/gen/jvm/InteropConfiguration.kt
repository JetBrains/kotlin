package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.indexer.NativeLibrary

/**
 * Describes the native library and the options for adjusting the Kotlin API to be generated for this library.
 */
class InteropConfiguration(
        val library: NativeLibrary,
        val pkgName: String,
        val excludedFunctions: Set<String>,
        val strictEnums: Set<String>,
        val nonStrictEnums: Set<String>
)