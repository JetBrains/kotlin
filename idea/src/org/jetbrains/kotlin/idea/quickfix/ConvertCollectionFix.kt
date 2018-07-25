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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class ConvertCollectionFix(element: KtExpression, val type: CollectionType) : KotlinQuickFixAction<KtExpression>(element) {
    override fun getFamilyName(): String = "Convert to ${type.displayName}"
    override fun getText() = "Convert expression to '${type.displayName}' by inserting '.${type.functionCall}'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val expression = element ?: return
        val factory = KtPsiFactory(expression)

        val replaced = expression.replaced(factory.createExpressionByPattern("$0.$1", expression, type.functionCall))
        editor?.caretModel?.moveToOffset(replaced.endOffset)
    }

    enum class CollectionType(
        val functionCall: String,
        val fqName: FqName,
        val literalFunctionName: String? = null,
        val emptyCollectionFunction: String? = null,
        private val nameOverride: String? = null
    ) {
        List("toList()", FqName("kotlin.collections.List"), "listOf", "emptyList"),
        Collection("toList()", FqName("kotlin.collections.Collection"), "listOf", "emptyList"),
        Iterable("toList()", FqName("kotlin.collections.Iterable"), "listOf", "emptyList"),
        MutableList("toMutableList()", FqName("kotlin.collections.MutableList")),
        Array("toTypedArray()", FqName("kotlin.Array"), "arrayOf", "emptyArray"),
        Sequence("asSequence()", FqName("kotlin.sequences.Sequence"), "sequenceOf", "emptySequence"),
        Set("toSet()", FqName("kotlin.collections.Set"), "setOf", "emptySet"),

        //specialized types must be last because iteration order is relevant for getCollectionType
        ArrayViaList("toList().toTypedArray()", FqName("kotlin.Array"), nameOverride = "Array"),
        ;

        val displayName get() = nameOverride ?: name

        fun specializeFor(sourceType: CollectionType) = when {
            this == Array && sourceType == Sequence -> ArrayViaList
            this == Array && sourceType == Iterable -> ArrayViaList
            else -> this
        }
    }

    companion object {
        private val TYPES = CollectionType.values()

        fun getConversionTypeOrNull(expressionType: KotlinType, expectedType: KotlinType): CollectionType? {
            val expressionCollectionType = expressionType.getCollectionType() ?: return null
            val expectedCollectionType = expectedType.getCollectionType() ?: return null

            val expressionTypeArg = expressionType.arguments.singleOrNull()?.type ?: return null
            val expectedTypeArg = expectedType.arguments.singleOrNull()?.type ?: return null
            if (!expressionTypeArg.isSubtypeOf(expectedTypeArg)) return null

            return expectedCollectionType.specializeFor(expressionCollectionType)
        }

        fun KotlinType.getCollectionType(acceptNullableTypes: Boolean = false): CollectionType? {
            if (isMarkedNullable && !acceptNullableTypes) return null
            return TYPES.firstOrNull { KotlinBuiltIns.isConstructedFromGivenClass(this, it.fqName) }
        }
    }
}
