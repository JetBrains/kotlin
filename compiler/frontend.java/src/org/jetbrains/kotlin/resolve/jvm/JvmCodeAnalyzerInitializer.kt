/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer

abstract class JvmCodeAnalyzerInitializer : CodeAnalyzerInitializer {
    abstract fun initialize(
        trace: BindingTrace,
        module: ModuleDescriptor,
        codeAnalyzer: KotlinCodeAnalyzer,
        languageVersionSettings: LanguageVersionSettings,
        jvmTarget: JvmTarget,
    )
}
