/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class TypeOfAnnotationMemberFix(
    typeReference: KtTypeReference,
    private val fixedType: String
) : KotlinQuickFixAction<KtTypeReference>(typeReference), CleanupFix {
    override fun getText(): String = KotlinBundle.message("replace.array.of.boxed.with.array.of.primitive")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val psiElement = element ?: return
        psiElement.replace(KtPsiFactory(psiElement).createType(fixedType))
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeReference = diagnostic.psiElement as? KtTypeReference ?: return null
            val type = typeReference.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference] ?: return null

            val itemType = type.getArrayItemType() ?: return null
            val itemTypeName = itemType.constructor.declarationDescriptor?.name?.asString() ?: return null
            val fixedArrayTypeText = if (itemType.isItemTypeToFix()) {
                "${itemTypeName}Array"
            } else {
                return null
            }

            return TypeOfAnnotationMemberFix(typeReference, fixedArrayTypeText)
        }

        private fun KotlinType.getArrayItemType(): KotlinType? {
            if (!KotlinBuiltIns.isArray(this)) {
                return null
            }

            val boxedType = arguments.singleOrNull() ?: return null
            if (boxedType.isStarProjection) {
                return null
            }

            return boxedType.type
        }

        private fun KotlinType.isItemTypeToFix() = KotlinBuiltIns.isByte(this)
                || KotlinBuiltIns.isChar(this)
                || KotlinBuiltIns.isShort(this)
                || KotlinBuiltIns.isInt(this)
                || KotlinBuiltIns.isLong(this)
                || KotlinBuiltIns.isFloat(this)
                || KotlinBuiltIns.isDouble(this)
                || KotlinBuiltIns.isBoolean(this)
    }
}