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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.prevSiblingOfSameType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

abstract class GenerateMembersHandler : AbstractGenerateMembersHandler<OverrideMemberChooserObject>() {

    override fun collectMembersToGenerate(classOrObject: KtClassOrObject): Collection<OverrideMemberChooserObject> {
        val descriptor = classOrObject.resolveToDescriptorIfAny() ?: return emptySet()
        return collectMembersToGenerate(descriptor, classOrObject.project)
    }

    protected abstract fun collectMembersToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject>

    override fun generateMembers(
        editor: Editor,
        classOrObject: KtClassOrObject,
        selectedElements: Collection<OverrideMemberChooserObject>,
        copyDoc: Boolean
    ) {
        return Companion.generateMembers(editor, classOrObject, selectedElements, copyDoc)
    }

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
                insertMembersAfterAndReformat(editor, classOrObject, selectedMemberDescriptors.keys)
                return
            }
            val offset = editor?.caretModel?.offset ?: classBody.startOffset
            val offsetCursorElement = PsiTreeUtil.findFirstParent(classBody.containingFile.findElementAt(offset)) {
                it.parent == classBody
            }
            if (offsetCursorElement != null && offsetCursorElement != classBody.rBrace) {
                insertMembersAfterAndReformat(editor, classOrObject, selectedMemberDescriptors.keys)
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
                    ?: (findCallableMemberBySignature(memberDescriptor)?.source?.getPsi() as? KtDeclaration)
                        ?.takeIf { it != classOrObject && it !is KtParameter }
                        ?.also { implementedElements[memberDescriptor] = it }
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

            insertMembersAfterAndReformat(editor, classOrObject, selectedMemberDescriptors.keys) { getAnchor(it) }
        }
    }
}
