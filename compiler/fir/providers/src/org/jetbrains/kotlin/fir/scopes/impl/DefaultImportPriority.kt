/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.resolve.ImportPath

enum class DefaultImportPriority {
    HIGH {
        override fun getAllDefaultImports(
            defaultImportsProvider: DefaultImportsProvider?,
            languageVersionSettings: LanguageVersionSettings
        ): List<ImportPath>? =
            defaultImportsProvider?.getDefaultImports(includeLowPriorityImports = false)
    },
    LOW {
        override fun getAllDefaultImports(
            defaultImportsProvider: DefaultImportsProvider?,
            languageVersionSettings: LanguageVersionSettings
        ): List<ImportPath>? =
            defaultImportsProvider?.defaultLowPriorityImports
    };

    abstract fun getAllDefaultImports(
        defaultImportsProvider: DefaultImportsProvider?,
        languageVersionSettings: LanguageVersionSettings
    ): List<ImportPath>?
}
