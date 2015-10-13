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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.lexer.JetTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import java.util.*

public class MakeOverriddenMemberOpenFix(declaration: JetDeclaration) : KotlinQuickFixAction<JetDeclaration>(declaration) {
    private val overriddenNonOverridableMembers = ArrayList<JetCallableDeclaration>()
    private val containingDeclarationsNames = ArrayList<String>()

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file) || file !is JetFile) {
            return false
        }

        // When running single test 'isAvailable()' is invoked multiple times, so we need to clear lists.
        overriddenNonOverridableMembers.clear()
        containingDeclarationsNames.clear()

        val descriptor = element.resolveToDescriptor()
        if (descriptor !is CallableMemberDescriptor) return false

        for (overriddenDescriptor in getAllDeclaredNonOverridableOverriddenDescriptors(
                descriptor)) {
            assert(overriddenDescriptor.kind == DECLARATION) { "Can only be applied to declarations." }
            val overriddenMember = DescriptorToSourceUtils.descriptorToDeclaration(overriddenDescriptor)
            if (overriddenMember == null || !QuickFixUtil.canModifyElement(overriddenMember) || overriddenMember !is JetCallableDeclaration) {
                return false
            }
            val containingDeclarationName = overriddenDescriptor.containingDeclaration.name.asString()
            overriddenNonOverridableMembers.add(overriddenMember)
            containingDeclarationsNames.add(containingDeclarationName)
        }
        return overriddenNonOverridableMembers.size() > 0
    }

    override fun getText(): String {
        if (overriddenNonOverridableMembers.size() == 1) {
            val name = containingDeclarationsNames.get(0) + "." + element.name
            return "Make $name $OPEN_KEYWORD"
        }

        Collections.sort(containingDeclarationsNames)
        val declarations = containingDeclarationsNames.subList(0, containingDeclarationsNames.size()-1).join(", ") + " and " +
            containingDeclarationsNames.last()
        return "Make '${element.name}' in $declarations open"
    }

    override fun getFamilyName(): String = "Add Modifier"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        for (overriddenMember in overriddenNonOverridableMembers) {
            overriddenMember.addModifier(OPEN_KEYWORD)
        }
    }

    companion object : JetSingleIntentionActionFactory() {

        private fun getAllDeclaredNonOverridableOverriddenDescriptors(
                callableMemberDescriptor: CallableMemberDescriptor): Collection<CallableMemberDescriptor> {
            val result = hashSetOf<CallableMemberDescriptor>()
            val nonOverridableOverriddenDescriptors = retainNonOverridableMembers(callableMemberDescriptor.overriddenDescriptors)
            for (overriddenDescriptor in nonOverridableOverriddenDescriptors) {
                when (overriddenDescriptor.kind) {
                    DECLARATION ->
                        result.add(overriddenDescriptor)

                    FAKE_OVERRIDE, DELEGATION ->
                        result.addAll(getAllDeclaredNonOverridableOverriddenDescriptors(overriddenDescriptor))

                    SYNTHESIZED -> {} /* do nothing */

                    else -> throw UnsupportedOperationException("Unexpected callable kind ${overriddenDescriptor.kind}")
                }
            }
            return result
        }

        private fun retainNonOverridableMembers(
                callableMemberDescriptors: Collection<CallableMemberDescriptor>): Collection<CallableMemberDescriptor> {
            return callableMemberDescriptors.filter { !it.modality.isOverridable }
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val declaration = diagnostic.psiElement.getNonStrictParentOfType<JetDeclaration>()!!
            return MakeOverriddenMemberOpenFix(declaration)
        }
    }
}
