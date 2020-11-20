/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class MakePrivateAndOverrideMemberFix(
    propertyOrParameter: KtDeclaration,
    private val memberToOverride: OverrideMemberChooserObject
) : KotlinQuickFixAction<KtDeclaration>(propertyOrParameter) {
    private val makePrivate = !propertyOrParameter.hasModifier(KtTokens.PRIVATE_KEYWORD)

    override fun getText(): String {
        val descriptor = memberToOverride.descriptor
        val implement = (descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE
        val name = descriptor.name.asString()
        return if (makePrivate) {
            KotlinBundle.message(
                "make.private.and.0.1",
                if (implement)
                    KotlinBundle.message("text.implements")
                else
                    KotlinBundle.message("text.overrides"),
                name
            )
        } else {
            (if (implement)
                KotlinBundle.message("highlighter.text.implements")
            else
                KotlinBundle.message("highlighter.text.overrides")) + " '$name'"
        }
    }

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (editor == null) return
        val element = element ?: return
        val containingClassOrObject = element.containingClassOrObject ?: return
        if (makePrivate) {
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
            val returnType = (element.descriptor as? CallableDescriptor)?.returnType ?: return null

            val isGetter = JvmAbi.isGetterName(memberNameToOverride)
            val isSetter = JvmAbi.isSetterName(memberNameToOverride)
            val memberToOverride = implementMembersHandler.collectMembersToGenerate(containingClassOrObject).firstOrNull {
                val descriptor = it.descriptor
                if (descriptor.name.asString() != memberNameToOverride) return@firstOrNull false
                val accessorReturnType = descriptor.returnType?.makeNotNullable() ?: return@firstOrNull false
                val params = descriptor.valueParameters
                (isGetter && params.isEmpty() && accessorReturnType == returnType) ||
                        (isSetter && params.singleOrNull()?.type?.makeNotNullable() == returnType && accessorReturnType.isUnit())
            } ?: return null

            return MakePrivateAndOverrideMemberFix(element, memberToOverride)
        }

        private val implementMembersHandler = ImplementMembersHandler()
    }
}
