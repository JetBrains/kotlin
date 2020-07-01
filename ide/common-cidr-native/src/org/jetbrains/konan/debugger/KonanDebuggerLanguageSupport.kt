package org.jetbrains.konan.debugger

import com.intellij.execution.configurations.RunProfile
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.OCDebuggerLanguageSupport
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper
import com.jetbrains.konan.debugger.KonanDebuggerTypesHelper

class KonanDebuggerLanguageSupport : OCDebuggerLanguageSupport() {
  // TODO disables frame-based language detection globally, even for non-kotlin projects
  override fun useFrameLanguageFromDebugger(profile: RunProfile?) = false

  override fun createTypesHelper(process: CidrDebugProcess): CidrDebuggerTypesHelper {
    return KonanDebuggerTypesHelper(process)
  }
}
