/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

enum class DefaultImportPriority {
    HIGH {
        override fun getAllDefaultImports(
            platformDependentAnalyzerServices: PlatformDependentAnalyzerServices?,
            languageVersionSettings: LanguageVersionSettings
        ): List<ImportPath>? =
            platformDependentAnalyzerServices?.getDefaultImports(languageVersionSettings, includeLowPriorityImports = false)?.let {
                it + ImportPath.fromString("kotlin.Throws")
            }
    },
    LOW {
        override fun getAllDefaultImports(
            platformDependentAnalyzerServices: PlatformDependentAnalyzerServices?,
            languageVersionSettings: LanguageVersionSettings
        ): List<ImportPath>? =
            platformDependentAnalyzerServices?.defaultLowPriorityImports
    };

    abstract fun getAllDefaultImports(
        platformDependentAnalyzerServices: PlatformDependentAnalyzerServices?,
        languageVersionSettings: LanguageVersionSettings
    ): List<ImportPath>?
}
