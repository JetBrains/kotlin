/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

class OverrideManyImplementationsFix(
    classOrObject: KtClassOrObject,
    private val memberToOverride: OverrideMemberChooserObject
) : KotlinQuickFixAction<KtClassOrObject>(classOrObject) {

    override fun getText() = "Override '${memberToOverride.descriptor.name.asString()}'"

    override fun getFamilyName() = "Override many implementations"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (editor == null) return
        val classOrObject = element ?: return
        OverrideImplementMembersHandler.generateMembers(editor, classOrObject, listOf(memberToOverride), false)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtClassOrObject>? {
            val casted = MANY_IMPL_MEMBER_NOT_IMPLEMENTED.cast(diagnostic)
            val classOrObject = casted.a as? KtClassOrObject ?: return null
            val callableMemberDescriptor = casted.b as? CallableMemberDescriptor ?: return null
            val memberToOverride = OverrideMemberChooserObject.create(
                classOrObject.project,
                callableMemberDescriptor,
                callableMemberDescriptor,
                OverrideMemberChooserObject.BodyType.EMPTY_OR_TEMPLATE
            )
            return OverrideManyImplementationsFix(classOrObject, memberToOverride)
        }
    }
}