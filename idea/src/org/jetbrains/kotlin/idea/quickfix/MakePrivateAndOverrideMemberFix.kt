/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class MakePrivateAndOverrideMemberFix(
    propertyOrParameter: KtDeclaration,
    private val memberNameToOverride: String,
    private val makePrivate: Boolean,
    private val implement: Boolean
) : KotlinQuickFixAction<KtDeclaration>(propertyOrParameter) {
    override fun getText(): String {
        return if (makePrivate) {
            "Make private and ${if (implement) "implements" else "overrides"} '$memberNameToOverride'"
        } else {
            "${if (implement) "Implements" else "Overrides"} '$memberNameToOverride'"
        }
    }

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (editor == null) return
        val element = element ?: return
        val containingClassOrObject = element.containingClassOrObject ?: return
        val memberToOverride = memberToOverride(containingClassOrObject, memberNameToOverride) ?: return

        if (makePrivate) {
            element.removeModifier(KtTokens.PUBLIC_KEYWORD)
            element.removeModifier(KtTokens.PROTECTED_KEYWORD)
            element.addModifier(KtTokens.PRIVATE_KEYWORD)
        }
        OverrideImplementMembersHandler.generateMembers(editor, containingClassOrObject, listOf(memberToOverride), false)
    }

    object AccidentalOverrideFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
            val casted = ErrorsJvm.ACCIDENTAL_OVERRIDE.cast(diagnostic)
            val element = casted.psiElement as? KtDeclaration ?: return null
            if (element !is KtProperty && element !is KtParameter) return null
            val containingClassOrObject = element.containingClassOrObject ?: return null
            val memberNameToOverride = casted.a.signature.name
            val memberToOverride = memberToOverride(containingClassOrObject, memberNameToOverride) ?: return null
            val makePrivate = !element.hasModifier(KtTokens.PRIVATE_KEYWORD)
            val implement = (memberToOverride.descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE
            return MakePrivateAndOverrideMemberFix(element, memberNameToOverride, makePrivate, implement)
        }
    }
}

private fun memberToOverride(classOrObject: KtClassOrObject, memberName: String): OverrideMemberChooserObject? {
    return ImplementMembersHandler().collectMembersToGenerate(classOrObject).firstOrNull { it.descriptor.name.asString() == memberName }
}