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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
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
): KotlinQuickFixAction<KtTypeReference>(typeReference), CleanupFix {
    override fun getText(): String  = "Replace array of boxed with array of primitive"

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
            }
            else {
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

        private fun KotlinType.isItemTypeToFix() =
                KotlinBuiltIns.isByte(this)
                || KotlinBuiltIns.isChar(this)
                || KotlinBuiltIns.isShort(this)
                || KotlinBuiltIns.isInt(this)
                || KotlinBuiltIns.isLong(this)
                || KotlinBuiltIns.isFloat(this)
                || KotlinBuiltIns.isDouble(this)
                || KotlinBuiltIns.isBoolean(this)
    }
}