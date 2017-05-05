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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.JvmCommonIntentionActionsFactory
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement


class KotlinCommonIntentionActionsFactory : JvmCommonIntentionActionsFactory() {
    override fun createChangeModifierAction(declaration: UDeclaration, modifier: String, shouldPresent: Boolean): IntentionAction? {
        val kModifierOwner = declaration.asKtElement<KtModifierListOwner>()
                             ?: throw IllegalArgumentException("$declaration is expected to contain KtLightElement with KtModifierListOwner")

        val (kToken, shouldPresentMapped) = if (PsiModifier.FINAL == modifier)
            KtTokens.OPEN_KEYWORD to !shouldPresent
        else
            javaPsiModifiersMapping[modifier] to shouldPresent

        if (kToken == null) return null
        return if (shouldPresentMapped)
            AddModifierFix.createIfApplicable(kModifierOwner, kToken)
        else
            RemoveModifierFix(kModifierOwner, kToken, false)
    }

    private inline fun <reified T : KtElement> UElement.asKtElement(): T? =
            (psi as? KtLightElement<*, *>?)?.kotlinOrigin as? T

    companion object {
        val javaPsiModifiersMapping = mapOf(
                PsiModifier.PRIVATE to KtTokens.PRIVATE_KEYWORD,
                PsiModifier.PUBLIC to KtTokens.PUBLIC_KEYWORD,
                PsiModifier.PROTECTED to KtTokens.PUBLIC_KEYWORD,
                PsiModifier.ABSTRACT to KtTokens.ABSTRACT_KEYWORD
        )

        val javaVisibilityMapping = mapOf(
                PsiModifier.PRIVATE to Visibilities.PRIVATE.displayName,
                PsiModifier.PUBLIC to "",
                PsiModifier.PROTECTED to Visibilities.PROTECTED.displayName,
                PsiModifier.PACKAGE_LOCAL to Visibilities.INTERNAL.displayName
        ).withDefault { Visibilities.DEFAULT_VISIBILITY }
    }

    override fun createAddMethodAction(u: UClass, methodName: String, visibilityModifier: String, returnType: PsiType, vararg parameters: PsiType): IntentionAction? {
        val returnTypeString: String = typeString(returnType).let {
            when {
                it.isEmpty() -> ""
                else -> ": $it"
            }
        }
        val paramsStr = parameters.mapIndexed { index, psiType -> "arg${index + 1}: ${typeString(psiType)}" }.joinToString()
        return object : LocalQuickFixAndIntentionActionOnPsiElement(u) {
            override fun getFamilyName(): String = "Add method"

            private val text = "Add method '$methodName' to '${u.name}'"

            override fun getText(): String = text

            override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
                val visibilityStr = javaVisibilityMapping.getValue(visibilityModifier)
                val psiFactory = KtPsiFactory(u)
                val function = psiFactory.createFunction("$visibilityStr fun $methodName($paramsStr)$returnTypeString{}")
                val ktClassOrObject = u.asKtElement<KtClassOrObject>()!!
                insertMembersAfter(null, ktClassOrObject, listOf(function), ktClassOrObject.declarations.lastOrNull())
            }
        }

    }

    private fun typeString(str: PsiType): String {
        return when (str) {
            PsiType.VOID -> ""
            PsiType.INT -> "kotlin.Int"
            PsiType.LONG -> "kotlin.Long"
            PsiType.SHORT -> "kotlin.Short"
            PsiType.BOOLEAN -> "kotlin.Boolean"
            PsiType.BYTE -> "kotlin.Byte"
            PsiType.CHAR -> "kotlin.Char"
            PsiType.DOUBLE -> "kotlin.Double"
            PsiType.FLOAT -> "kotlin.Float"
            else -> str.canonicalText
        }
    }
}