/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import java.nio.file.Path

class KmmFileSystemWizardService : FileSystemWizardService {
    override fun createDirectory(path: Path): TaskResult<Unit> =
        safe {
            runWriteAction<Unit> {
                VfsUtil.createDirectoryIfMissing(path.toString())
            }
        }

    override fun createFile(path: Path, text: String): TaskResult<Unit> =
        safe {
            if (path.toFile().exists()) return@safe

            runWriteAction {
                val directoryPath = path.parent
                val directory =
                    VfsUtil.createDirectoryIfMissing(directoryPath.toFile().toString())!!
                val virtualFile = directory.createChildData(this, path.fileName.toString())
                VfsUtil.saveText(
                    virtualFile,
                    StringUtil.convertLineSeparators(
                        text
                    )
                )
            }
        }
}