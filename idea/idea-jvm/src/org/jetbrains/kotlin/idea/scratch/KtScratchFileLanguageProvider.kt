/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.scratch.compile.KtCompilingExecutor
import org.jetbrains.kotlin.idea.scratch.output.InlayScratchOutputHandler
import org.jetbrains.kotlin.idea.scratch.repl.KtScratchReplExecutor
import org.jetbrains.kotlin.psi.KtFile

class KtScratchFileLanguageProvider : ScratchFileLanguageProvider() {
    override fun createFile(project: Project, editor: TextEditor): ScratchFile? {
        return KtScratchFile(project, editor)
    }

    override fun createReplExecutor(file: ScratchFile) = KtScratchReplExecutor(file)
    override fun createCompilingExecutor(file: ScratchFile) = KtCompilingExecutor(file)

    override fun getOutputHandler() = InlayScratchOutputHandler
}