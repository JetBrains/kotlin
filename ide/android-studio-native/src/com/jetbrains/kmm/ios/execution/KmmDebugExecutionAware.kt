package com.jetbrains.kmm.ios.execution

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.OCPathManagerCustomization
import com.jetbrains.mpp.debugger.DebugExecutionAware
import com.jetbrains.mpp.debugger.LLDBBackendBase.Companion.DEBUG_SERVER_PATH_KEY

class KmmDebugExecutionAware : DebugExecutionAware() {
    override fun getParams(project: Project): Map<String, String> = mapOf(
        DEBUG_SERVER_PATH_KEY to OCPathManagerCustomization.getInstance().getBinFile(LOCAL_DEBUG_SERVER).absolutePath
    )
}