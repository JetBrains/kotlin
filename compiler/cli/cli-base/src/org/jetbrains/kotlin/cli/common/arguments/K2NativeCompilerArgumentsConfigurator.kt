/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersion

class K2NativeCompilerArgumentsConfigurator : CommonKlibBasedCompilerArgumentsConfigurator() {
    override fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        require(this is K2NativeCompilerArguments)
        super.configureAnalysisFlags(arguments, reporter, languageVersion).apply {
            val optInList = (get(AnalysisFlags.optIn) as List<*>?).orEmpty()
            putAnalysisFlag(AnalysisFlags.optIn, optInList + listOf("kotlin.ExperimentalUnsignedTypes"))
            if (printIr)
                phasesToDumpAfter = arrayOf("ALL")
            if (metadataKlib) {
                putAnalysisFlag(AnalysisFlags.metadataCompilation, true)
            }
        }
    }
}
