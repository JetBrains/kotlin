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
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

class MakeOverriddenMemberOpenFix(declaration: KtDeclaration) : KotlinQuickFixAction<KtDeclaration>(declaration) {
    private val overriddenNonOverridableMembers = ArrayList<SmartPsiElementPointer<KtCallableDeclaration>>()
    private val containingDeclarationsNames = ArrayList<String>()

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false

        if (!super.isAvailable(project, editor, file) || file !is KtFile) {
            return false
        }

        // When running single test 'isAvailable()' is invoked multiple times, so we need to clear lists.
        overriddenNonOverridableMembers.clear()
        containingDeclarationsNames.clear()

        val descriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableMemberDescriptor ?: return false

        for (overriddenDescriptor in getAllDeclaredNonOverridableOverriddenDescriptors(descriptor)) {
            assert(overriddenDescriptor.kind == DECLARATION) { "Can only be applied to declarations." }
            val overriddenMember = DescriptorToSourceUtils.descriptorToDeclaration(overriddenDescriptor)
            if (overriddenMember == null || !overriddenMember.canRefactor() || overriddenMember !is KtCallableDeclaration) {
                return false
            }
            val containingDeclarationName = overriddenDescriptor.containingDeclaration.name.asString()
            overriddenNonOverridableMembers.add(overriddenMember.createSmartPointer())
            containingDeclarationsNames.add(containingDeclarationName)
        }
        return overriddenNonOverridableMembers.size > 0
    }

    override fun getText(): String {
        val element = element ?: return ""

        if (overriddenNonOverridableMembers.size == 1) {
            val name = containingDeclarationsNames[0] + "." + element.name
            return "Make $name $OPEN_KEYWORD"
        }

        Collections.sort(containingDeclarationsNames)
        val declarations = containingDeclarationsNames.subList(0, containingDeclarationsNames.size - 1).joinToString(", ") + " and " +
                           containingDeclarationsNames.last()
        return "Make '${element.name}' in $declarations open"
    }

    override fun getFamilyName(): String = "Add Modifier"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        for (overriddenMember in overriddenNonOverridableMembers) {
            overriddenMember.element?.addModifier(OPEN_KEYWORD)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {

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
            return callableMemberDescriptors.filter { !it.isOverridable }
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val declaration = diagnostic.psiElement.getNonStrictParentOfType<KtDeclaration>()!!
            return MakeOverriddenMemberOpenFix(declaration)
        }
    }
}
