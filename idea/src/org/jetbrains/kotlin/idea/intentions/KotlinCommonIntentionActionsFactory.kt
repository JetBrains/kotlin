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

import com.intellij.codeInsight.intention.*
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target.CONSTRUCTOR
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

    override fun createAddBeanPropertyActions(uClass: UClass,
                                              propertyName: String,
                                              visibilityModifier: String,
                                              propertyType: PsiType,
                                              setterRequired: Boolean,
                                              getterRequired: Boolean): List<IntentionAction> {

        fun addPropertyFix(lateinit: Boolean = false) =
                Fix(uClass,
                    "Add property",
                    "Add '${if (lateinit) "lateinit " else ""}" +
                    "${if (setterRequired) "var" else "val"}' property '$propertyName' to '${uClass.name}'")
                { uClass ->
                    val visibilityStr = javaVisibilityMapping.getValue(visibilityModifier)
                    val psiFactory = KtPsiFactory(uClass)
                    val modifiersString = if (lateinit) "lateinit $visibilityStr" else visibilityStr
                    val function = psiFactory.createProperty(
                            modifiersString,
                            propertyName,
                            typeString(propertyType),
                            setterRequired,
                            if (lateinit) null else "TODO(\"initialize me\")")
                    val ktClassOrObject = uClass.asKtElement<KtClassOrObject>()!!
                    insertMembersAfter(null, ktClassOrObject, listOf(function), null)
                }

        if (setterRequired)
            return listOf(addPropertyFix(), addPropertyFix(lateinit = true))
        else
            return listOf(addPropertyFix())
    }

    override fun createAddCallableMemberActions(info: MethodInsertionInfo): List<IntentionAction> =
            when (info) {
                is MethodInsertionInfo.Method ->
                    createAddMethodAction(info)?.let { listOf(it) } ?: emptyList()

                is MethodInsertionInfo.Constructor ->
                    createAddConstructorActions(info)
            }

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

        fun typeString(str: PsiType): String {
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
                typeName = JavaToKotlinClassMap.mapJavaToKotlin(FqName(str.canonicalText), DefaultBuiltIns.Instance)?.fqNameSafe?.asString()

            return typeName ?: str.canonicalText
        }
    }

    private inline fun <reified T : KtElement> UElement.asKtElement(): T? =
            (psi as? KtLightElement<*, *>?)?.kotlinOrigin as? T

    private fun CallableBuilder.paramsFromInfo(info: MethodInsertionInfo) {
        for ((index, param) in info.parameters.withIndex()) {
            param(param.name ?: "arg${index + 1}", typeString(param.type))
        }
    }

    private fun createAddMethodAction(info: MethodInsertionInfo.Method): IntentionAction? {
        val visibilityStr = info.modifiers.map { javaVisibilityMapping.get(it) ?: it }.joinToString(" ")
        val functionString = CallableBuilder(FUNCTION).apply {
            modifier(visibilityStr)
            typeParams()
            name(info.name)
            paramsFromInfo(info)
            info.returnType.let {
                when (it) {
                    PsiType.VOID -> noReturnType()
                    else -> returnType(typeString(it))
                }
            }
            blockBody("")
        }

        return Fix(info.containingClass, "Add method", "Add method '${info.name}' to '${info.containingClass.name}'") {
            uClass ->
            val psiFactory = KtPsiFactory(uClass)
            val function = psiFactory.createFunction(functionString.asString())
            val ktClassOrObject = uClass.asKtElement<KtClassOrObject>()!!
            insertMembersAfter(null, ktClassOrObject, listOf(function), ktClassOrObject.declarations.lastOrNull())
        }

    }

    private fun createAddConstructorActions(info: MethodInsertionInfo.Constructor): List<IntentionAction> {
        val constructorString = CallableBuilder(CONSTRUCTOR).apply {
            modifier("")
            typeParams()
            name()
            paramsFromInfo(info)
            noReturnType()
            blockBody("")
        }.asString()
        val primaryConstructor = info.containingClass.asKtElement<KtClass>()!!.primaryConstructor

        val addConstructorAction = if (primaryConstructor == null)
            Fix(info.containingClass,
                "Add method",
                "Add primary constructor to '${info.containingClass.name}'",
                { uClass ->
                    val psiFactory = KtPsiFactory(uClass)
                    val constructor = psiFactory.createSecondaryConstructor(constructorString)
                    val ktClass = uClass.asKtElement<KtClass>()!!
                    val newPrimaryConstructor = ktClass.createPrimaryConstructorIfAbsent()
                    newPrimaryConstructor.valueParameterList!!.replace(constructor.valueParameterList!!)
                    ShortenReferences.DEFAULT.process(newPrimaryConstructor)
                })
        else Fix(info.containingClass,
                 "Add method",
                 "Add secondary constructor to '${info.containingClass.name}'",
                 { uClass ->
                     val psiFactory = KtPsiFactory(uClass)
                     val constructor = psiFactory.createSecondaryConstructor(constructorString)
                     val ktClassOrObject = uClass.asKtElement<KtClassOrObject>()!!
                     insertMembersAfter(null, ktClassOrObject, listOf(constructor), null)
                 })

        val changePrimaryConstructorAction = run {
            if (primaryConstructor == null) return@run null
            QuickFixFactory.getInstance()
                    .createChangeMethodSignatureFromUsageFix(
                            LightClassUtil.getLightClassMethod(primaryConstructor)!!,
                            fakeParametersExpressions(info.parameters),
                            PsiSubstitutor.EMPTY, info.containingClass, false, 2
                    ).takeIf { it.isAvailable(info.containingClass.project, null, info.containingClass.containingFile) }
        }

        return listOf(changePrimaryConstructorAction, addConstructorAction).filterNotNull()
    }

    private fun fakeParametersExpressions(parameters: List<PsiParameter>): Array<PsiExpression> =
            JavaPsiFacade.getElementFactory(parameters.first().project)
                    .createParameterList(
                            parameters.map { it.name }.toTypedArray(),
                            parameters.map { it.type }.toTypedArray()
                    ).parameters.map { FakeExpressionFromParameter(it) }.toTypedArray()

    private class FakeExpressionFromParameter(private val psiParam: PsiParameter) : PsiReferenceExpressionImpl() {

        override fun getText(): String = psiParam.name!!

        override fun getProject(): Project = psiParam.project

        override fun getParent(): PsiElement = psiParam.parent

        override fun getType(): PsiType? = psiParam.type

        override fun isValid(): Boolean = true

        override fun getContainingFile(): PsiFile = psiParam.containingFile

        override fun getReferenceName(): String? = psiParam.name

        override fun resolve(): PsiElement? = psiParam
    }

    private class Fix(uClass: UClass, private val familyName: String, private val text: String, private val action: (uClass: UClass) -> Unit) : LocalQuickFixAndIntentionActionOnPsiElement(uClass) {
        override fun getFamilyName(): String = familyName

        override fun getText(): String = text

        override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) =
                action(startElement as UClass)
    }

}

