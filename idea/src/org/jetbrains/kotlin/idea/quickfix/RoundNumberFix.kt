/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isDouble
import org.jetbrains.kotlin.types.typeUtil.isFloat
import org.jetbrains.kotlin.types.typeUtil.isInt
import org.jetbrains.kotlin.types.typeUtil.isLong

class RoundNumberFix(
    element: KtExpression,
    type: KotlinType,
    private val disableIfAvailable: IntentionAction? = null
) : KotlinQuickFixAction<KtExpression>(element), LowPriorityAction {

    private val isTarget = type.isLongOrInt() && element.analyze(BodyResolveMode.PARTIAL).getType(element)?.isDoubleOrFloat() == true

    private val roundFunction = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type).let { "roundTo$it" }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile) =
        disableIfAvailable?.isAvailable(project, editor, file) != true && isTarget

    override fun getText() = "Round using $roundFunction()"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val replaced = element.replaced(KtPsiFactory(file).createExpressionByPattern("$0.$roundFunction()", element))
        file.resolveImportReference(FqName("kotlin.math.$roundFunction")).firstOrNull()?.also {
            ImportInsertHelper.getInstance(project).importDescriptor(file, it)
        }
        editor?.caretModel?.moveToOffset(replaced.endOffset)
    }

    private fun KotlinType.isLongOrInt() = this.isLong() || this.isInt()

    private fun KotlinType.isDoubleOrFloat() = this.isDouble() || this.isFloat()
}