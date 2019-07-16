/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.intellij.execution.configurations.RunProfile
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.CidrDebuggerLanguageSupport
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper
import com.jetbrains.konan.debugger.KonanDebuggerTypesHelper
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver.StandardDebuggerLanguage.C // TODO: C -> KOTLIN_NATIVE

class KonanDebuggerLanguageSupport : CidrDebuggerLanguageSupport() {
    // TODO disables frame-based language detection globally, even for non-kotlin projects
    override fun useFrameLanguageFromDebugger(profile: RunProfile?) = false

    override fun createTypesHelper(process: CidrDebugProcess): CidrDebuggerTypesHelper {
        return KonanDebuggerTypesHelper(process)
    }

    override fun getSupportedDebuggerLanguages(): MutableSet<DebuggerDriver.DebuggerLanguage> {
        return mutableSetOf(C)
    }
}