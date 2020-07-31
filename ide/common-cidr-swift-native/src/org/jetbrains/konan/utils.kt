package org.jetbrains.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.findModule
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.konan.isNative

fun VirtualFile.isCommonOrIos(project: Project): Boolean =
    findModule(project)?.platform?.let { it.isCommon() || it.isNative() } ?: false
