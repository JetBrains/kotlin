/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty

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
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog("Cannot decompile ${file.name}", "Decompiler error")
                    }
                    return@runWriteAction
                }

                val root: VirtualFile = getOrCreateDummyRoot()
                val decompiledFileName = FileUtil.getNameWithoutExtension(file.name) + ".decompiled.java"
                val result = DummyFileSystem.getInstance().createChildFile(null, root, decompiledFileName)
                VfsUtil.saveText(result, decompiledText)

                result.isKotlinDecompiledFile = true

                OpenFileDescriptor(file.project, result).navigate(true)
            }
        }
    }
}

val KOTLIN_DECOMPILED_FOLDER = "kotlinDecompiled"
val KOTLIN_DECOMPILED_ROOT = "dummy://$KOTLIN_DECOMPILED_FOLDER"

var VirtualFile.isKotlinDecompiledFile: Boolean by NotNullableUserDataProperty(Key.create("IS_KOTLIN_DECOMPILED_FILE"), false)

fun getOrCreateDummyRoot(): VirtualFile =
    VirtualFileManager.getInstance().refreshAndFindFileByUrl(KOTLIN_DECOMPILED_ROOT) ?:
       DummyFileSystem.getInstance().createRoot(KOTLIN_DECOMPILED_FOLDER)

