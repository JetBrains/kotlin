/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import java.nio.file.Path

class IdeaFileSystemWizardService : FileSystemWizardService, IdeaWizardService {
    override fun createDirectory(path: Path): TaskResult<Unit> = safe {
        runWriteAction<Unit> {
            VfsUtil.createDirectoryIfMissing(path.toString())
        }
    }

    override fun createFile(path: Path, text: String): TaskResult<Unit> = safe {
        runWriteAction {
            val directoryPath = path.parent
            val directory =
                VfsUtil.createDirectoryIfMissing(directoryPath.toFile().toString())!!
            val virtualFile = directory.createChildData(this, path.fileName.toString())
            VfsUtil.saveText(virtualFile, text)
        }
    }
}