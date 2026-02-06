/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonKlibBasedCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class CommonKlibBasedCompilerArguments : CommonCompilerArguments() {
    @Argument(
        value = "-Xklib-abi-version",
        valueDescription = "<version>",
        description = """Specify the custom ABI version to be written in KLIB. This option is intended only for tests.
Warning: This option does not affect KLIB ABI. Neither allows it making a KLIB backward-compatible with older ABI versions.
The only observable effect is that a custom ABI version is written to KLIB manifest file.""",
    )
    var customKlibAbiVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xklib-duplicated-unique-name-strategy",
        valueDescription = "{deny|allow-all-with-warning|allow-first-with-warning}",
        description = "Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value.",
    )
    var duplicatedUniqueNameStrategy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xklib-enable-signature-clash-checks",
        description = "Enable signature uniqueness checks.",
    )
    var enableSignatureClashChecks: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-ir-inliner",
        valueDescription = "{intra-module|full|disabled|default}",
        description = """Set the mode of the experimental IR inliner on the first compilation stage.
- `intra-module` mode enforces inlining of the functions only from the compiled module
- `full` mode enforces inlining of all functions (from the compiled module and from all dependencies)
   Warning: This mode will trigger setting the `pre-release` flag for the compiled library.
- `disabled` mode completely disables the IR inliner
- `default` mode lets the IR inliner run in `intra-module`, `full` or `disabled` mode based on the current language version
        """,
    )
    var irInlinerBeforeKlibSerialization: String = "default"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-normalize-absolute-path",
        description = "Normalize absolute paths in klibs.",
    )
    var normalizeAbsolutePath: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-relative-path-base",
        description = """Relativize all the paths stored in a klib using the given path prefixes.
The supplied prefixes should be absolute paths to the directories containing the source code files.
Note: The prefixes are applied in the same order as they are passed in this CLI argument.""",
    )
    var relativePathBases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-zip-file-accessor-cache-limit",
        description = "Maximum number of klibs that can be cached during compilation. Default is 64.",
    )
    var klibZipFileAccessorCacheLimit: String = "64"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xpartial-linkage",
        valueDescription = "{enable|disable}",
        description = "Use partial linkage mode.",
    )
    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xpartial-linkage-loglevel",
        valueDescription = "{info|warning|error}",
        description = "Define the compile-time log level for partial linkage.",
    )
    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xskip-library-special-compatibility-checks",
        description = "Skip library compatibility checks for stdlib and kotlin.test library.",
    )
    var skipLibrarySpecialCompatibilityChecks: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

}
