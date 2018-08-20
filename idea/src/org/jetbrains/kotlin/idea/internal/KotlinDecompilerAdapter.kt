/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile

fun showDecompiledCode(sourceFile: KtFile) {
    ProgressManager.getInstance().run(KotlinBytecodeDecompilerTask(sourceFile))
}

class KotlinBytecodeDecompilerTask(val file: KtFile) : Task.Backgroundable(file.project, "Decompile kotlin bytecode") {
    override fun run(indicator: ProgressIndicator) {
        val decompilerService = KotlinDecompilerService.getInstance() ?: return

        indicator.text = "Decompiling ${file.name}"

        val decompiledText = try {
            decompilerService.decompile(file)
        } catch (e: DecompileFailedException) {
            null
        }

        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                if (!file.isValid || file.project.isDisposed) return@runWriteAction

                if (decompiledText == null) {
                    Messages.showErrorDialog("Cannot decompile ${file.name}", "Decompiler error")
                    return@runWriteAction
                }

                val root = getOrCreateDummyRoot()
                val decompiledFileName = FileUtil.getNameWithoutExtension(file.name) + ".decompiled.java"
                val result = DummyFileSystem.getInstance().createChildFile(null, root, decompiledFileName)
                VfsUtil.saveText(result, decompiledText)

                OpenFileDescriptor(file.project, result).navigate(true)
            }
        }
    }
}

val KOTLIN_DECOMPILED_FOLDER = "kotlinDecompiled"
val KOTLIN_DECOMPILED_ROOT = "dummy://$KOTLIN_DECOMPILED_FOLDER"

fun getOrCreateDummyRoot(): VirtualFile =
    VirtualFileManager.getInstance().refreshAndFindFileByUrl(KOTLIN_DECOMPILED_ROOT) ?:
       DummyFileSystem.getInstance().createRoot(KOTLIN_DECOMPILED_FOLDER)

