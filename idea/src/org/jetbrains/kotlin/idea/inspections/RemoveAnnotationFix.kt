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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile

class RemoveAnnotationFix(private val name: String, annotationEntry: KtAnnotationEntry)
        : KotlinQuickFixAction<KtAnnotationEntry>(annotationEntry) {

    override fun getText() = name

    override fun getFamilyName() = name

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.delete()
    }

    object JvmOverloads : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): RemoveAnnotationFix? {
            val annotationEntry = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            return RemoveAnnotationFix("Remove @JvmOverloads annotation", annotationEntry)
        }
    }
}