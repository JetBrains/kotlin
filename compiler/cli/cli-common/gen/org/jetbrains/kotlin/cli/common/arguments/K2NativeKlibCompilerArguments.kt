/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/NativeKlibCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

class K2NativeKlibCompilerArguments : CommonKlibBasedCompilerArguments() {
    @Argument(
        value = "-Xexport-kdoc",
        description = "Export KDoc entries in the klib.",
    )
    var exportKDoc: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfake-override-validator",
        description = "Enable the IR fake override validator.",
    )
    var fakeOverrideValidator: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xheader-klib-path",
        description = "Save a klib that only contains the public ABI to the given path.",
    )
    var headerKlibPath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xkonan-data-dir",
        description = "Custom path to the location of konan distributions.",
    )
    var konanDataDir: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xmanifest-native-targets",
        description = "Comma-separated list that will be written as the value of 'native_targets' property in the .klib manifest. Unknown values are discarded.",
    )
    var manifestNativeTargets: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xrefines-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for refined modules (modules whose 'expect' declarations this module can actualize).",
    )
    var refinesPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xshort-module-name",
        valueDescription = "<name>",
        description = "A short name used to denote this library in the IDE and in a generated Objective-C header.",
    )
    var shortModuleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xwrite-dependencies-of-produced-klib-to",
        valueDescription = "<path>",
        description = "Write file containing the paths of dependencies used during klib compilation to the provided path",
    )
    var writeDependenciesOfProducedKlibTo: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-friend-modules",
        valueDescription = "<path>",
        description = "Paths to friend modules.",
    )
    var friendModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-include-binary",
        shortName = "-ib",
        deprecatedName = "-includeBinary",
        valueDescription = "<path>",
        description = "Pack the given external binary into the klib.",
    )
    var includeBinaries: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-library",
        shortName = "-l",
        valueDescription = "<path>",
        description = "Link with the given library.",
        delimiter = Argument.Delimiters.none,
    )
    var libraries: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-manifest",
        valueDescription = "<path>",
        description = "Provide a manifest addend file.",
    )
    var manifestFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-module-name",
        deprecatedName = "-module_name",
        valueDescription = "<name>",
        description = "Specify a name for the compilation module.",
    )
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-native-library",
        shortName = "-nl",
        deprecatedName = "-nativelibrary",
        valueDescription = "<path>",
        description = "Include the given native bitcode library.",
        delimiter = Argument.Delimiters.none,
    )
    var nativeLibraries: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-no-default-libs",
        deprecatedName = "-nodefaultlibs",
        description = "Don't link the libraries from dist/klib automatically.",
    )
    var nodefaultlibs: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-nopack",
        description = "Don't pack the library into a klib file.",
    )
    var nopack: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-nostdlib",
        description = "Don't link with the stdlib.",
    )
    var nostdlib: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-output",
        shortName = "-o",
        valueDescription = "<name>",
        description = "Output name.",
    )
    var outputName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-target",
        valueDescription = "<target>",
        description = "Set the hardware target.",
    )
    var target: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @get:Transient
    @field:kotlin.jvm.Transient
    override val configurator: CommonCompilerArgumentsConfigurator = K2NativeKlibCompilerArgumentsConfigurator()

    override fun copyOf(): Freezable = copyK2NativeKlibCompilerArguments(this, K2NativeKlibCompilerArguments())
}
