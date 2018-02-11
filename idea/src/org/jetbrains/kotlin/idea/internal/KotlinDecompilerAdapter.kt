/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.internal

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile

fun showDecompiledCode(sourceFile: KtFile) {
    val decompilerService = KotlinDecompilerService.getInstance() ?: return
    val decompiledCode = try {
        decompilerService.decompile(sourceFile)
    } catch (e: DecompileFailedException) {
        null
    }

    if (decompiledCode == null) {
        Messages.showErrorDialog("Cannot decompile ${sourceFile.name}", "Decompiler error")
        return
    }

    runWriteAction {
        val root = getOrCreateDummyRoot()
        val decompiledFileName = FileUtil.getNameWithoutExtension(sourceFile.name) + ".decompiled.java"
        val result = DummyFileSystem.getInstance().createChildFile(null, root, decompiledFileName)
        VfsUtil.saveText(result, decompiledCode)

        OpenFileDescriptor(sourceFile.project, result).navigate(true)
    }
}

val KOTLIN_DECOMPILED_FOLDER = "kotlinDecompiled"
val KOTLIN_DECOMPILED_ROOT = "dummy://$KOTLIN_DECOMPILED_FOLDER"

fun getOrCreateDummyRoot(): VirtualFile =
    VirtualFileManager.getInstance().refreshAndFindFileByUrl(KOTLIN_DECOMPILED_ROOT) ?:
       DummyFileSystem.getInstance().createRoot(KOTLIN_DECOMPILED_FOLDER)

