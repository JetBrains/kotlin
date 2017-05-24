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
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target.FUNCTION
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
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

        val javaVisibilityMapping: Map<String, String> = mapOf(
                PsiModifier.PRIVATE to Visibilities.PRIVATE.displayName,
                PsiModifier.PUBLIC to "",
                PsiModifier.PROTECTED to Visibilities.PROTECTED.displayName,
                PsiModifier.PACKAGE_LOCAL to Visibilities.INTERNAL.displayName
        ).withDefault { Visibilities.DEFAULT_VISIBILITY.displayName }
    }

    override fun createAddMethodAction(uClass: UClass, methodName: String, visibilityModifier: String, returnType: PsiType, vararg parameters: PsiType): IntentionAction? {
        return object : LocalQuickFixAndIntentionActionOnPsiElement(uClass) {
            override fun getFamilyName(): String = "Add method"

            private val text = "Add method '$methodName' to '${uClass.name}'"

            override fun getText(): String = text

            override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
                val visibilityStr = javaVisibilityMapping.getValue(visibilityModifier)
                val psiFactory = KtPsiFactory(uClass)
                val functionString = psiFactory.createFunction(CallableBuilder(FUNCTION).apply {
                    modifier(visibilityStr)
                    typeParams()
                    name(methodName)
                    for ((index, psiType) in parameters.withIndex()) {
                        param("arg${index + 1}", typeString(psiType))
                    }
                    if (returnType == PsiType.VOID)
                        noReturnType()
                    else
                        returnType(typeString(returnType))
                    blockBody("")
                }.asString())
                val ktClassOrObject = uClass.asKtElement<KtClassOrObject>()!!
                insertMembersAfter(null, ktClassOrObject, listOf(functionString), ktClassOrObject.declarations.lastOrNull())
            }
        }

    }

    override fun createAddBeanPropertyActions(uClass: UClass, propertyName: String, visibilityModifier: String, propertyType: PsiType, setterRequired: Boolean, getterRequired: Boolean): Array<IntentionAction> {

        return arrayOf(object : LocalQuickFixAndIntentionActionOnPsiElement(uClass) {
            override fun getFamilyName(): String = "Add property"

            private val text = "Add '${if (setterRequired) "var" else "val"}' property '$propertyName' to '${uClass.name}'"

            override fun getText(): String = text

            override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
                val visibilityStr = javaVisibilityMapping.getValue(visibilityModifier)
                val psiFactory = KtPsiFactory(uClass)
                val function = psiFactory.createProperty(visibilityStr, propertyName, typeString(propertyType), setterRequired, null)
                val ktClassOrObject = uClass.asKtElement<KtClassOrObject>()!!
                insertMembersAfter(null, ktClassOrObject, listOf(function), null)
            }
        })
    }


    private fun typeString(str: PsiType): String {
        var typeName: String? = when (str) {
            PsiType.VOID -> ""
            PsiType.INT -> "kotlin.Int"
            PsiType.LONG -> "kotlin.Long"
            PsiType.SHORT -> "kotlin.Short"
            PsiType.BOOLEAN -> "kotlin.Boolean"
            PsiType.BYTE -> "kotlin.Byte"
            PsiType.CHAR -> "kotlin.Char"
            PsiType.DOUBLE -> "kotlin.Double"
            PsiType.FLOAT -> "kotlin.Float"
            else -> null
        }
        if (typeName == null)
            typeName = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(str.canonicalText), DefaultBuiltIns.Instance)?.fqNameSafe?.asString()

        return typeName ?: str.canonicalText
    }
}