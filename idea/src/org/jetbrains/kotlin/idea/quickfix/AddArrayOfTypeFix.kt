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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType

class AddArrayOfTypeFix(expression: KtExpression, expectedType: KotlinType) : KotlinQuickFixAction<KtExpression>(expression) {

    private val prefix = if (KotlinBuiltIns.isArray(expectedType)) {
        "arrayOf"
    }
    else {
        val typeName = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(expectedType)
        "${typeName.decapitalize()}Of"

    }

    override fun getText() = "Add $prefix wrapper"
    override fun getFamilyName() = "Add arrayOf wrapper"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val arrayOfExpression = KtPsiFactory(project).createExpressionByPattern("$0($1)", prefix, element)
        element.replace(arrayOfExpression)
    }
}