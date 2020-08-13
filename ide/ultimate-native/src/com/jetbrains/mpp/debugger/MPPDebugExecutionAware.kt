package com.jetbrains.mpp.debugger

import com.intellij.openapi.project.Project
import com.jetbrains.mpp.MPPWorkspace
import com.jetbrains.mpp.debugger.LLDBBackendBase.Companion.DEBUG_SERVER_ARGS_KEY
import com.jetbrains.mpp.debugger.LLDBBackendBase.Companion.DEBUG_SERVER_PATH_KEY
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

class MPPDebugExecutionAware : DebugExecutionAware() {
    override fun getParams(project: Project): Map<String, String> {
        val lldbHome = MPPWorkspace.getInstance(project).lldbHome ?: return emptyMap()

        val params = HashMap<String, String>()

        when (HostManager.host) {
            KonanTarget.MACOS_X64 -> {
                params[DEBUG_SERVER_PATH_KEY] = lldbHome.resolve(LOCAL_DEBUG_SERVER).toString()

            }
            KonanTarget.LINUX_X64 -> {
                params[DEBUG_SERVER_PATH_KEY] = lldbHome.resolve("bin/lldb-server").toString()
                params[DEBUG_SERVER_ARGS_KEY] = "g"
            }
        }

        return params
    }
}