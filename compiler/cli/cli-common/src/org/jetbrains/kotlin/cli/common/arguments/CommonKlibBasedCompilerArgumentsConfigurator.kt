/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.library.KotlinAbiVersion

open class CommonKlibBasedCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureExtraLanguageFeatures(arguments: CommonCompilerArguments, map: HashMap<LanguageFeature, LanguageFeature.State>) {
        require(arguments is CommonKlibBasedCompilerArguments)
        if (arguments.irInlinerBeforeKlibSerialization) {
            map[LanguageFeature.IrInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
        }
    }
}

fun parseCustomKotlinAbiVersion(customKlibAbiVersion: String?, collector: MessageCollector): KotlinAbiVersion? {
    val versionParts = customKlibAbiVersion?.split('.') ?: return null
    if (versionParts.size != 3) {
        collector.report(
            CompilerMessageSeverity.ERROR,
            "Invalid ABI version format. Expected format: <major>.<minor>.<patch>"
        )
        return null
    }
    val version = versionParts.mapNotNull { it.toIntOrNull() }
    val validNumberRegex = Regex("(0|[1-9]\\d{0,2})")
    if (versionParts.any { !it.matches(validNumberRegex) } || version.any { it !in 0..255 }) {
        collector.report(
            CompilerMessageSeverity.ERROR,
            "Invalid ABI version numbers. Each part must be in the range 0..255."
        )
        return null
    }
    return KotlinAbiVersion(version[0], version[1], version[2])
}
