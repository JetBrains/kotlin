/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    val decompiledCode = decompilerService.decompile(sourceFile)
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

