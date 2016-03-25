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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isInterface

private fun KotlinType.renderShort() = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(this)

class LetImplementInterfaceFix(
        element: KtExpression,
        private val expectedType: KotlinType
) : KotlinQuickFixAction<KtExpression>(element) {

    private val expressionType: KotlinType?
        get() = element.analyze(BodyResolveMode.PARTIAL).getType(element)
    private val expressionTypeDeclaration: PsiElement?
        get() = expressionType?.constructor?.declarationDescriptor?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) }

    override fun getFamilyName() = "Let type implement interface"
    override fun getText(): String {
        val expressionType = expressionType
        val verb = if (expressionType?.isInterface() ?: true) "extend" else "implement"
        return "Let '${expressionType?.renderShort()}' $verb interface '${expectedType.renderShort()}'"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false

        if (!expectedType.isInterface()) return false
        val expressionSuperTypes = expressionType?.constructor?.supertypes?.map(KotlinType::getConstructor) ?: return false
        if (expectedType.constructor in expressionSuperTypes) return false
        if (expressionTypeDeclaration !is KtClassOrObject) return false

        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val declaration = expressionTypeDeclaration as? KtClassOrObject ?: return
        val superTypeEntry = KtPsiFactory(element).createSuperTypeEntry(IdeDescriptorRenderers.SOURCE_CODE.renderType(expectedType))
        val entryElement = declaration.addSuperTypeListEntry(superTypeEntry)
        ShortenReferences.DEFAULT.process(entryElement)
    }

}