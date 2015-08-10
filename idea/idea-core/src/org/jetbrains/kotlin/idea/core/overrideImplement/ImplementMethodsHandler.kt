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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.resolve.OverrideResolver

public class ImplementMethodsHandler : OverrideImplementMethodsHandler(), IntentionAction {
    override fun collectMethodsToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject> {
        return OverrideResolver.getMissingImplementations(descriptor)
                .map { OverrideMemberChooserObject.create(project, it, it) }
    }

    override fun getChooserTitle() = "Implement Members"

    override fun getNoMethodsFoundHint() = "No methods to implement have been found"

    override fun getText() = JetBundle.message("implement.members")
    override fun getFamilyName() = JetBundle.message("implement.members")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = isValidFor(editor, file)
}
