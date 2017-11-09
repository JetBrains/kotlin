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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

private val valueRanges = mapOf(
        KotlinBuiltIns.FQ_NAMES._byte to Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong(),
        KotlinBuiltIns.FQ_NAMES._short to Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong(),
        KotlinBuiltIns.FQ_NAMES._int to Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong(),
        KotlinBuiltIns.FQ_NAMES._long to Long.MIN_VALUE..Long.MAX_VALUE
)

class WrongPrimitiveLiteralFix(element: KtConstantExpression, type: KotlinType) : KotlinQuickFixAction<KtExpression>(element) {

    private val typeName = DescriptorUtils.getFqName(type.constructor.declarationDescriptor!!)
    private val expectedTypeIsFloat = KotlinBuiltIns.isFloat(type)
    private val expectedTypeIsDouble = KotlinBuiltIns.isDouble(type)

    private val constValue = run {
        val shouldInlineCosntVals = element.languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)
        ExpressionCodegen.getPrimitiveOrStringCompileTimeConstant(
                element, element.analyze(BodyResolveMode.PARTIAL), shouldInlineCosntVals)?.value as? Number
    }

    private val fixedExpression = buildString {
        if (expectedTypeIsFloat || expectedTypeIsDouble) {
            append(constValue)
            if (expectedTypeIsFloat) {
                append('F')
            }
            else if ('.' !in this) {
                append(".0")
            }
        }
        else {
            if (constValue is Float || constValue is Double) {
                append(constValue.toLong())
            }
            else {
                append(element.text.trimEnd('l', 'L'))
            }

            if (KotlinBuiltIns.isLong(type)) {
                append('L')
            }
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        if (constValue == null) return false
        if (expectedTypeIsFloat || expectedTypeIsDouble) return true

        if (constValue is Float || constValue is Double) {
            val value = constValue.toDouble()
            if (value != Math.floor(value)) return false
            if (value !in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) return false
        }

        return constValue.toLong() in valueRanges[typeName] ?: return false
    }

    override fun getFamilyName() = "Change to correct primitive type"
    override fun getText() = "Change to '$fixedExpression'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val expressionToInsert = KtPsiFactory(file).createExpression(fixedExpression)
        val newExpression = element.replaced(expressionToInsert)
        editor?.caretModel?.moveToOffset(newExpression.endOffset)
    }
}
