/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios.execution

import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.CidrDebuggerEditorsProvider
import com.jetbrains.cidr.execution.debugger.CidrDebuggerLanguageSupport
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper
import com.jetbrains.konan.debugger.KonanDebuggerTypesHelper

class KonanDebuggerLanguageSupport : CidrDebuggerLanguageSupport() {
    override fun getSupportedDebuggerLanguages(): Set<DebuggerDriver.DebuggerLanguage> {
        // currently Kotlin/Native presents itself as a C in debug information
        return setOf(DebuggerDriver.StandardDebuggerLanguage.C)
    }

    override fun createEditor(project: Project, profile: RunProfile?): XDebuggerEditorsProvider? {
        return CidrDebuggerEditorsProvider()
    }

    override fun createTypesHelper(process: CidrDebugProcess): CidrDebuggerTypesHelper {
        return KonanDebuggerTypesHelper(process)
    }

    override fun useFrameLanguageFromDebugger(profile: RunProfile?) = false
}

