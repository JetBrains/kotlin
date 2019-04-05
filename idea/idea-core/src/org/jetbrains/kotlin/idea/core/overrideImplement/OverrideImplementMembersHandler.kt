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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.prevSiblingOfSameType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

abstract class OverrideImplementMembersHandler : LanguageCodeInsightActionHandler {

    fun collectMembersToGenerate(classOrObject: KtClassOrObject): Collection<OverrideMemberChooserObject> {
        val descriptor = classOrObject.resolveToDescriptorIfAny() ?: return emptySet()
        return collectMembersToGenerate(descriptor, classOrObject.project)
    }

    protected abstract fun collectMembersToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject>

    private fun showOverrideImplementChooser(
        project: Project,
        members: Array<OverrideMemberChooserObject>
    ): MemberChooser<OverrideMemberChooserObject>? {
        val chooser = MemberChooser(members, true, true, project)
        chooser.title = getChooserTitle()
        chooser.show()
        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null
        return chooser
    }

    protected abstract fun getChooserTitle(): String

    protected open fun isValidForClass(classOrObject: KtClassOrObject) = true

    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        if (file !is KtFile) return false
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<KtClassOrObject>()
        return classOrObject != null && isValidForClass(classOrObject)
    }

    protected abstract fun getNoMembersFoundHint(): String

    fun invoke(project: Project, editor: Editor, file: PsiFile, implementAll: Boolean) {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<KtClassOrObject>() ?: return

        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val members = collectMembersToGenerate(classOrObject)
        if (members.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMembersFoundHint())
            return
        }

        val copyDoc: Boolean
        val selectedElements: Collection<OverrideMemberChooserObject>
        if (implementAll) {
            selectedElements = members
            copyDoc = false
        } else {
            val chooser = showOverrideImplementChooser(project, members.toTypedArray()) ?: return
            selectedElements = chooser.selectedElements ?: return
            copyDoc = chooser.isCopyJavadoc
        }
        if (selectedElements.isEmpty()) return

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        generateMembers(editor, classOrObject, selectedElements, copyDoc)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        invoke(project, editor, file, implementAll = ApplicationManager.getApplication().isUnitTestMode)
    }

    override fun startInWriteAction(): Boolean = false

    companion object {
        fun generateMembers(
            editor: Editor?,
            classOrObject: KtClassOrObject,
            selectedElements: Collection<OverrideMemberChooserObject>,
            copyDoc: Boolean
        ) {
            val selectedMemberDescriptors = selectedElements.associate { it.generateMember(classOrObject, copyDoc) to it.descriptor }

            val classBody = classOrObject.body
            if (classBody == null) {
                insertMembersAfter(editor, classOrObject, selectedMemberDescriptors.keys)
                return
            }
            val offset = editor?.caretModel?.offset ?: classBody.startOffset
            val offsetCursorElement = PsiTreeUtil.findFirstParent(classBody.containingFile.findElementAt(offset)) {
                it.parent == classBody
            }
            if (offsetCursorElement != null && offsetCursorElement != classBody.rBrace) {
                insertMembersAfter(editor, classOrObject, selectedMemberDescriptors.keys)
                return
            }
            val classLeftBrace = classBody.lBrace

            val allSuperMemberDescriptors = selectedMemberDescriptors.values
                .mapNotNull { it.containingDeclaration as? ClassDescriptor }
                .associateWith {
                    DescriptorUtils.getAllDescriptors(it.unsubstitutedMemberScope).filterIsInstance<CallableMemberDescriptor>()
                }

            val implementedElements = mutableMapOf<CallableMemberDescriptor, KtDeclaration>()

            fun ClassDescriptor.findElement(memberDescriptor: CallableMemberDescriptor): KtDeclaration? {
                return implementedElements[memberDescriptor]
                    ?: (findCallableMemberBySignature(memberDescriptor)?.source?.getPsi() as? KtDeclaration)?.also {
                        implementedElements[memberDescriptor] = it
                    }
            }

            fun getAnchor(selectedElement: KtDeclaration): PsiElement? {
                val lastElement = classOrObject.declarations.lastOrNull()
                val selectedMemberDescriptor = selectedMemberDescriptors[selectedElement] ?: return lastElement
                val superMemberDescriptors = allSuperMemberDescriptors[selectedMemberDescriptor.containingDeclaration] ?: return lastElement
                val index = superMemberDescriptors.indexOf(selectedMemberDescriptor)
                if (index == -1) return lastElement
                val classDescriptor = classOrObject.descriptor as? ClassDescriptor ?: return lastElement

                val upperElement = ((index - 1) downTo 0).firstNotNullResult {
                    classDescriptor.findElement(superMemberDescriptors[it])
                }
                if (upperElement != null) return upperElement

                val lowerElement = ((index + 1) until superMemberDescriptors.size).firstNotNullResult {
                    classDescriptor.findElement(superMemberDescriptors[it])
                }
                if (lowerElement != null) return lowerElement.prevSiblingOfSameType() ?: classLeftBrace

                return lastElement
            }

            insertMembersAfter(editor, classOrObject, selectedMemberDescriptors.keys) { getAnchor(it) }
        }
    }
}
