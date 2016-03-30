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
import org.jetbrains.kotlin.types.typeUtil.supertypes

private fun KotlinType.renderShort() = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(this)

private fun KotlinType.getSuperTypesRecursive() = constructor.supertypes.let { it + it.flatMap { it.supertypes() } }

class LetImplementInterfaceFix(
        element: KtExpression,
        expectedType: KotlinType
) : KotlinQuickFixAction<KtExpression>(element) {

    private val isAvailable = run {
        if (!expectedType.isInterface()) return@run false

        val expressionSuperTypes = expressionType?.getSuperTypesRecursive()?.map(KotlinType::getConstructor) ?: return@run false
        if (expectedType.constructor in expressionSuperTypes) return@run false
        if (expressionTypeDeclaration !is KtClassOrObject) return@run false

        return@run true
    }

    private val expectedTypeRenderedShort = expectedType.renderShort()
    private val expectedTypeRendered = IdeDescriptorRenderers.SOURCE_CODE.renderType(expectedType)

    private val expressionType: KotlinType?
        get() = element.analyze(BodyResolveMode.PARTIAL).getType(element)
    private val expressionTypeDeclaration: PsiElement?
        get() = expressionType?.constructor?.declarationDescriptor?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) }

    override fun getFamilyName() = "Let type implement interface"
    override fun getText(): String {
        val expressionType = expressionType
        val verb = if (expressionType?.isInterface() ?: true) "extend" else "implement"
        return "Let '${expressionType?.renderShort()}' $verb interface '$expectedTypeRenderedShort'"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) && isAvailable
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val declaration = expressionTypeDeclaration as? KtClassOrObject ?: return
        val superTypeEntry = KtPsiFactory(element).createSuperTypeEntry(expectedTypeRendered)
        val entryElement = declaration.addSuperTypeListEntry(superTypeEntry)
        ShortenReferences.DEFAULT.process(entryElement)
    }

}