/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.DuplicatedUniqueNameStrategies.ALL
import org.jetbrains.kotlin.cli.common.arguments.DuplicatedUniqueNameStrategies.DENY
import org.jetbrains.kotlin.cli.common.arguments.DuplicatedUniqueNameStrategies.FIRST

abstract class CommonKlibBasedCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }

    @Argument(
        value = "-Xklib-relative-path-base",
        description = "Provide a base path to compute the source's relative paths in klib (default is empty)."
    )
    var relativePathBases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-normalize-absolute-path",
        description = "Normalize absolute paths in klibs."
    )
    var normalizeAbsolutePath = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-enable-signature-clash-checks",
        description = "Enable signature uniqueness checks."
    )
    var enableSignatureClashChecks = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xpartial-linkage", valueDescription = "{enable|disable}", description = "Use partial linkage mode.")
    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xpartial-linkage-loglevel", valueDescription = "{info|warning|error}", description = "Define the compile-time log level for partial linkage.")
    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xklib-double-inlining",
        description = "Turn on experimental double-inlining mode.\nWarning: This feature is unstable."
    )
    var experimentalDoubleInlining = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-duplicated-unique-name-strategy",
        valueDescription = "{$DENY|$ALL|$FIRST}",
        description = "Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value."
    )
    var duplicatedUniqueNameStrategy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }
}

object DuplicatedUniqueNameStrategies {
    const val DENY = "deny"
    const val ALL = "allow-all-with-warning"
    const val FIRST = "allow-first-with-warning"
}
