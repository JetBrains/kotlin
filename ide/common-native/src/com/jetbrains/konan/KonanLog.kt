/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

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
