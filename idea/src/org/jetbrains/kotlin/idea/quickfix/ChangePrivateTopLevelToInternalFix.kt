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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils

public class ChangePrivateTopLevelToInternalFix(element: JetModifierListOwner, private val elementName: String) : JetIntentionAction<JetModifierListOwner>(element), CleanupFix {
    override fun getText() = "Make $elementName internal"
    override fun getFamilyName() = "Make top-level declaration internal"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        element.addModifier(JetTokens.INTERNAL_KEYWORD)
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            @Suppress("UNCHECKED_CAST")
            val factory = diagnostic.factory as DiagnosticFactory3<*, DeclarationDescriptor, *, DeclarationDescriptor>
            val descriptor = factory.cast(diagnostic).c
            if (!DescriptorUtils.isTopLevelDeclaration(descriptor) ||
                descriptor !is DeclarationDescriptorWithVisibility ||
                descriptor.visibility != Visibilities.PRIVATE) return null

            val declaration = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? JetModifierListOwner ?: return null
            return ChangePrivateTopLevelToInternalFix(declaration, descriptor.name.asString())
        }
    }
}
