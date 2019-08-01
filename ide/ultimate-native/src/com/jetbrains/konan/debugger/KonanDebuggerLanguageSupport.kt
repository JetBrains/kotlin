/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.CidrDebuggerLanguageSupport
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver.StandardDebuggerLanguage.C
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper

class KonanDebuggerLanguageSupport : CidrDebuggerLanguageSupport() {
    override fun createTypesHelper(process: CidrDebugProcess): CidrDebuggerTypesHelper {
        return KonanDebuggerTypesHelper(process)
    }

    override fun getSupportedDebuggerLanguages(): MutableSet<DebuggerDriver.DebuggerLanguage> {
        return mutableSetOf(C)
    }
}`