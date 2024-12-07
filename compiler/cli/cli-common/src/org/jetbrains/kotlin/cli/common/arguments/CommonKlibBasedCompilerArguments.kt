/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion

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
        value = "-Xverify-ir-visibility-after-inlining",
        description = """Check for visibility violations in IR when validating it after the function inlining phase.
Only has effect if '-Xverify-ir' is not 'none'.
This flag is deprecated and will soon be removed in favor of '-Xverify-ir-visibility'.
""",
    )
    var verifyIrVisibilityAfterInlining: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-no-double-inlining",
        description = "Turn off double-inlining mode."
    )
    var noDoubleInlining = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-duplicated-unique-name-strategy",
        valueDescription = "{${DuplicatedUniqueNameStrategy.ALL_ALIASES}}",
        description = "Klib dependencies usage strategy when multiple KLIBs has same `unique_name` property value."
    )
    var duplicatedUniqueNameStrategy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xklib-ir-inliner",
        description = "Enable experimental support to invoke IR Inliner before Klib serialization."
    )
    var irInlinerBeforeKlibSerialization = false
        set(value) {
            checkFrozen()
            field = value
        }



    override fun configureExtraLanguageFeatures(map: HashMap<LanguageFeature, LanguageFeature.State>) {
        super.configureExtraLanguageFeatures(map)
        if (irInlinerBeforeKlibSerialization) {
            map[LanguageFeature.IrInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
        }
    }
}
