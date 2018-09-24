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

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class AddActualFix(
    actualClassOrObject: KtClassOrObject,
    expectedClassOrObject: KtClassOrObject
) : KotlinQuickFixAction<KtClassOrObject>(actualClassOrObject) {

    private val expectedClassPointer = expectedClassOrObject.createSmartPointer()

    override fun getFamilyName() = text

    override fun getText() = "Add missing actual members"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val expectedClass = expectedClassPointer.element ?: return
        val factory = KtPsiFactory(element)
        val pureActualClass = factory.generateClassOrObjectByExpectedClass(
            project, expectedClass, actualNeeded = true,
            existingDeclarations = element.declarations +
                    element.primaryConstructor?.valueParameters?.filter { it.hasValOrVar() }.orEmpty() +
                    listOfNotNull(element.primaryConstructor)
        )
        for (declaration in pureActualClass.declarations) {
            element.addDeclaration(declaration)
        }
        val primaryConstructor = pureActualClass.primaryConstructor
        if (element.primaryConstructor == null && primaryConstructor != null) {
            element.addAfter(primaryConstructor, element.nameIdentifier)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val incompatibleMap = DiagnosticFactory.cast(diagnostic, Errors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS).b
            val expectedClassDescriptor = incompatibleMap.firstOrNull()?.first?.containingDeclaration as? ClassDescriptor
                ?: return null
            val expectedClassOrObject = DescriptorToSourceUtils.descriptorToDeclaration(expectedClassDescriptor) as? KtClassOrObject
                ?: return null
            return (diagnostic.psiElement as? KtClassOrObject)?.let { AddActualFix(it, expectedClassOrObject) }
        }
    }
}