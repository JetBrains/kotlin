/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.debugger

import com.intellij.execution.configurations.RunProfile
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.OCDebuggerLanguageSupport
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper

class KonanDebuggerLanguageSupport : OCDebuggerLanguageSupport() {
  // TODO disables frame-based language detection globally, even for non-kotlin projects
  override fun useFrameLanguageFromDebugger(profile: RunProfile?) = false

  override fun createTypesHelper(process: CidrDebugProcess): CidrDebuggerTypesHelper {
    return KonanDebuggerTypesHelper(process)
  }
}
