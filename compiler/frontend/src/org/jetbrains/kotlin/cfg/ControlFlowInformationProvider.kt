/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor
import org.jetbrains.kotlin.types.KotlinType

interface ControlFlowInformationProvider {
    fun checkForLocalClassOrObjectMode()

    fun checkDeclaration()

    fun checkFunction(expectedReturnType: KotlinType?)

    interface Factory {
        fun createControlFlowInformationProvider(
            declaration: KtElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            diagnosticSuppressor: PlatformDiagnosticSuppressor,
            enumWhenTracker: EnumWhenTracker
        ): ControlFlowInformationProvider
    }
}
