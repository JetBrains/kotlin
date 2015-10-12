/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.testIntegration

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testIntegration.TestCreator

class KotlinTestCreator : TestCreator {
    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        return KotlinCreateTestIntention().isAvailable(project, editor, file)
    }

    override fun createTest(project: Project, editor: Editor, file: PsiFile) {
        KotlinCreateTestIntention().invoke(project, editor, file)
    }
}