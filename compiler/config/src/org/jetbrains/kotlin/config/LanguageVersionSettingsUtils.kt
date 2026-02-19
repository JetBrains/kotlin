/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config


val LanguageVersionSettings.isLibraryToSourceAnalysisEnabled: Boolean
    get() = getFlag(AnalysisFlags.libraryToSourceAnalysis)

val LanguageVersionSettings.areExpectActualClassesStable: Boolean
    get() {
        return getFlag(AnalysisFlags.muteExpectActualClassesWarning) || supportsFeature(LanguageFeature.ExpectActualClasses)
    }
