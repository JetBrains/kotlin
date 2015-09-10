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

package org.jetbrains.kotlin.idea.quickfix.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.replaceWith.ClassUsageReplacementStrategy
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

public class DecapitalizedAnnotationFix(
        element: JetSimpleNameExpression,
        private val classDescriptor: ClassDescriptor,
        private val replacer: () -> JetElement
) : JetIntentionAction<JetSimpleNameExpression>(element), CleanupFix {
    override fun getFamilyName() = "Replace deprecated decapitalized annotations"
    override fun getText() = "Replace with '${classDescriptor.fqNameSafe.asString()}'"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        replacer()
    }

    companion object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val diagnosticWithParameters = Errors.DEPRECATED_DECAPITALIZED_ANNOTATION.cast(diagnostic)
            val classDescriptor = diagnosticWithParameters.a
            val element = diagnosticWithParameters.psiElement

            val replacement = JetPsiFactory(element).createType(classDescriptor.fqNameSafe.asString()).typeElement as JetUserType
            val replacer = ClassUsageReplacementStrategy(replacement).createReplacer(element) ?: return null

            return DecapitalizedAnnotationFix(element, classDescriptor, replacer)
        }
    }
}
