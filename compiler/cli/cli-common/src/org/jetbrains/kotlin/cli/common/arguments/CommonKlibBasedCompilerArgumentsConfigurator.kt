/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.KlibIrInlinerMode
import org.jetbrains.kotlin.config.LanguageFeature

open class CommonKlibBasedCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        collector: MessageCollector,
    ) {
        require(arguments is CommonKlibBasedCompilerArguments)

        val klibIrInlinerMode = KlibIrInlinerMode.fromString(arguments.irInlinerBeforeKlibSerialization)
        when (klibIrInlinerMode) {
            KlibIrInlinerMode.INTRA_MODULE -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
            }
            KlibIrInlinerMode.FULL -> {
                map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
                map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
            }
            KlibIrInlinerMode.DISABLED -> {}
            null -> {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unknown value for parameter -Xklib-ir-inliner: '${arguments.irInlinerBeforeKlibSerialization}'. Value should be one of ${KlibIrInlinerMode.availableValues()}"
                )
            }
        }
    }
}
