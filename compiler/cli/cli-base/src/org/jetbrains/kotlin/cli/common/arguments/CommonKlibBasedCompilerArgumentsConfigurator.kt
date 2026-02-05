/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.KlibIrInlinerMode
import org.jetbrains.kotlin.config.LanguageFeature

open class CommonKlibBasedCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        reporter: Reporter,
    ) {
        require(arguments is CommonKlibBasedCompilerArguments)

        val klibIrInlinerMode = KlibIrInlinerMode.fromString(arguments.irInlinerBeforeKlibSerialization)
        when (klibIrInlinerMode) {
            KlibIrInlinerMode.DEFAULT -> {
                // Do nothing. Rely on the default language feature states.
            }
            KlibIrInlinerMode.INTRA_MODULE -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
            }
            KlibIrInlinerMode.FULL -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
                // TODO(KT-71896): Drop this reporting when the cross-inlining becomes enabled by default.
                reporter.info(
                    "`-Xklib-ir-inliner=full` will trigger setting the `pre-release` flag for the compiled library."
                )
            }
            KlibIrInlinerMode.DISABLED -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.DISABLED
            }
            null -> {
                reporter.reportError(
                    "Unknown value for parameter -Xklib-ir-inliner: '${arguments.irInlinerBeforeKlibSerialization}'. Value should be one of ${KlibIrInlinerMode.availableValues()}"
                )
            }
        }
    }
}
