/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
// TODO: move to scripting-idea module
package org.jetbrains.kotlin.idea.script

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.extensions.ExtensionPointName
import kotlin.script.experimental.api.ScriptDiagnostic

interface ScriptDiagnosticFixProvider {

    fun provideFixes(annotation: ScriptDiagnostic): List<IntentionAction>

    companion object {
        val EP_NAME = ExtensionPointName.create<ScriptDiagnosticFixProvider>("org.jetbrains.kotlin.scriptDiagnosticFixProvider")
    }
}
