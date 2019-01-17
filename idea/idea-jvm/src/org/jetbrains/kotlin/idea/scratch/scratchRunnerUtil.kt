/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.externalSystem.service.project.autoimport.ConfigurationFileCrcFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.UserDataProperty

fun isScratchChanged(project: Project, file: VirtualFile) : Boolean {
    val beforeLastRun = file.crcWithoutSpaces
    val newCrc = ConfigurationFileCrcFactory(project, file).create()
    if (beforeLastRun != newCrc) {
        file.crcWithoutSpaces = newCrc
        return true
    }
    return false
}

private var VirtualFile.crcWithoutSpaces by UserDataProperty(Key.create<Long>("CRC_WITHOUT_SPACES"))