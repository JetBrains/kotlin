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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.containsStarProjections
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isInterface

class LetImplementInterfaceFix(
        element: KtClassOrObject,
        expectedType: KotlinType,
        expressionType: KotlinType
) : KotlinQuickFixAction<KtClassOrObject>(element), LowPriorityAction {

    private fun KotlinType.renderShort() = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(this)

    private val expectedTypeName: String

    private val expectedTypeNameSourceCode: String

    private val prefix: String

    private val validExpectedType = with (expectedType) {
        isInterface() &&
        !containsStarProjections() &&
        constructor !in TypeUtils.getAllSupertypes(expressionType).map(KotlinType::constructor)
    }

    init {
        val expectedTypeNotNullable = TypeUtils.makeNotNullable(expectedType)
        expectedTypeName = expectedTypeNotNullable.renderShort()
        expectedTypeNameSourceCode = IdeDescriptorRenderers.SOURCE_CODE.renderType(expectedTypeNotNullable)

        val verb = if (expressionType.isInterface()) "extend" else "implement"
        val typeDescription = if (element.isObjectLiteral()) "the anonymous object" else "'${expressionType.renderShort()}'"
        prefix = "Let $typeDescription $verb"

    }

    override fun getFamilyName() = "Let type implement interface"
    override fun getText() = "$prefix interface '$expectedTypeName'"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile) =
            super.isAvailable(project, editor, file) && validExpectedType

    override fun startInWriteAction() = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val superTypeEntry = KtPsiFactory(element).createSuperTypeEntry(expectedTypeNameSourceCode)
        runWriteAction {
            val entryElement = element.addSuperTypeListEntry(superTypeEntry)
            ShortenReferences.DEFAULT.process(entryElement)
        }

        val implementMembersHandler = ImplementMembersHandler()
        if (implementMembersHandler.collectMembersToGenerate(element).isEmpty()) return

        if (editor != null) {
            editor.caretModel.moveToOffset(element.textRange.startOffset)
            val containingFile = element.containingFile
            FileEditorManager.getInstance(project).openFile(containingFile.virtualFile, true)
            implementMembersHandler.invoke(project, editor, containingFile)
        }
    }
}
