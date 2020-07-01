package com.jetbrains.konan

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.openapi.diagnostic.Logger
import java.io.File

object KonanLog {
    val LOG = Logger.getInstance("#com.jetbrains.konan.KonanLog")

    fun logReadVersion(tool: String, exec: File?) {
        if (exec == null) {
            LOG.debug("$tool.readVersion() file: null")
        } else {
            LOG.debug(tool + ".readVersion() file: '" + exec + "' not " + if (!exec.isFile) "file" else "executable")
        }
    }

    fun logReadVersion(tool: String, exec: File?, output: String?) {
        LOG.debug("$tool.readVersion() file: '$exec' output: $output")
    }

    val MESSAGES = NotificationGroup("Kotlin/Native Messages", NotificationDisplayType.BALLOON, true)
}
