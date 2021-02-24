/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinBundle
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
import org.jetbrains.kotlin.types.typeUtil.isUnsignedNumberType
import kotlin.math.floor

private val valueRanges = mapOf(
    StandardNames.FqNames._byte to Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong(),
    StandardNames.FqNames._short to Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong(),
    StandardNames.FqNames._int to Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong(),
    StandardNames.FqNames._long to Long.MIN_VALUE..Long.MAX_VALUE
)

class WrongPrimitiveLiteralFix(element: KtConstantExpression, type: KotlinType) : KotlinQuickFixAction<KtExpression>(element) {

    private val typeName = DescriptorUtils.getFqName(type.constructor.declarationDescriptor!!)
    private val expectedTypeIsFloat = KotlinBuiltIns.isFloat(type)
    private val expectedTypeIsDouble = KotlinBuiltIns.isDouble(type)
    private val expectedTypeIsUnsigned = type.isUnsignedNumberType()

    private val constValue = run {
        val shouldInlineConstVals = element.languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)
        ExpressionCodegen.getPrimitiveOrStringCompileTimeConstant(
            element, element.analyze(BodyResolveMode.PARTIAL), shouldInlineConstVals
        )?.value as? Number
    }

    private val fixedExpression = buildString {
        if (expectedTypeIsFloat || expectedTypeIsDouble) {
            append(constValue)
            if (expectedTypeIsFloat) {
                append('F')
            } else if ('.' !in this) {
                append(".0")
            }
        } else if (expectedTypeIsUnsigned) {
            append(constValue)
            append('u')
        } else {
            if (constValue is Float || constValue is Double) {
                append(constValue.toLong())
            } else {
                append(element.text.trimEnd('l', 'L', 'u'))
            }

            if (KotlinBuiltIns.isLong(type)) {
                append('L')
            }
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        if (constValue == null) return false
        if (expectedTypeIsFloat || expectedTypeIsDouble || expectedTypeIsUnsigned) return true

        if (constValue is Float || constValue is Double) {
            val value = constValue.toDouble()
            if (value != floor(value)) return false
            if (value !in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) return false
        }

        return constValue.toLong() in valueRanges[typeName] ?: return false
    }

    override fun getFamilyName() = KotlinBundle.message("change.to.correct.primitive.type")
    override fun getText() = KotlinBundle.message("change.to.0", fixedExpression)

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val expressionToInsert = KtPsiFactory(file).createExpression(fixedExpression)
        val newExpression = element.replaced(expressionToInsert)
        editor?.caretModel?.moveToOffset(newExpression.endOffset)
    }
}
