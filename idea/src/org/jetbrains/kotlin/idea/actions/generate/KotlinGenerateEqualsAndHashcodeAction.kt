/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull
import java.util.*

fun ClassDescriptor.findDeclaredEquals(checkSupers: Boolean): FunctionDescriptor? {
    return findDeclaredFunction("equals", checkSupers) {
        it.valueParameters.singleOrNull()?.type == it.builtIns.nullableAnyType && it.typeParameters.isEmpty()
    }
}

fun ClassDescriptor.findDeclaredHashCode(checkSupers: Boolean): FunctionDescriptor? {
    return findDeclaredFunction("hashCode", checkSupers) { it.valueParameters.isEmpty() && it.typeParameters.isEmpty() }
}

class KotlinGenerateEqualsAndHashcodeAction : KotlinGenerateMemberActionBase<KotlinGenerateEqualsAndHashcodeAction.Info>() {
    companion object {
        private val LOG = Logger.getInstance(KotlinGenerateEqualsAndHashcodeAction::class.java)
    }

    class Info(
        val needEquals: Boolean,
        val needHashCode: Boolean,
        val classDescriptor: ClassDescriptor,
        val variablesForEquals: List<VariableDescriptor>,
        val variablesForHashCode: List<VariableDescriptor>
    )

    override fun isValidForClass(targetClass: KtClassOrObject): Boolean {
        return targetClass is KtClass
                && targetClass !is KtEnumEntry
                && !targetClass.isEnum()
                && !targetClass.isAnnotation()
                && !targetClass.isInterface()
    }

    override fun prepareMembersInfo(klass: KtClassOrObject, project: Project, editor: Editor?): Info? {
        if (klass !is KtClass) throw AssertionError("Not a class: ${klass.getElementTextWithContext()}")

        val context = klass.analyzeWithContent()
        val classDescriptor = context.get(BindingContext.CLASS, klass) ?: return null

        val equalsDescriptor = classDescriptor.findDeclaredEquals(false)
        val hashCodeDescriptor = classDescriptor.findDeclaredHashCode(false)

        var needEquals = equalsDescriptor == null
        var needHashCode = hashCodeDescriptor == null
        if (!needEquals && !needHashCode) {
            if (!confirmMemberRewrite(klass, equalsDescriptor!!, hashCodeDescriptor!!)) return null

            runWriteAction {
                try {
                    equalsDescriptor.source.getPsi()?.delete()
                    hashCodeDescriptor.source.getPsi()?.delete()
                    needEquals = true
                    needHashCode = true
                } catch (e: IncorrectOperationException) {
                    LOG.error(e)
                }
            }
        }

        val properties = getPropertiesToUseInGeneratedMember(klass)

        if (properties.isEmpty() || ApplicationManager.getApplication().isUnitTestMode) {
            val descriptors = properties.map { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as VariableDescriptor }
            return Info(needEquals, needHashCode, classDescriptor, descriptors, descriptors)
        }

        return with(KotlinGenerateEqualsWizard(project, klass, properties, needEquals, needHashCode)) {
            if (!klass.hasExpectModifier() && !showAndGet()) return null

            Info(needEquals,
                 needHashCode,
                 classDescriptor,
                 getPropertiesForEquals().map { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as VariableDescriptor },
                 getPropertiesForHashCode().map { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as VariableDescriptor })
        }
    }

    private fun generateClassLiteralsNotEqual(paramName: String, targetClass: KtClassOrObject): String {
        val defaultExpression = "javaClass != $paramName?.javaClass"
        if (!targetClass.languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences)) return defaultExpression
        return when {
            targetClass.platform.isJs() -> "other == null || this::class.js != $paramName::class.js"
            targetClass.platform.isCommon() -> "other == null || this::class != $paramName::class"
            else -> defaultExpression
        }
    }

    private fun generateClassLiteral(targetClass: KtClassOrObject): String {
        val defaultExpression = "javaClass"
        if (!targetClass.languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences)) return defaultExpression
        return when {
            targetClass.platform.isJs() -> "this::class.js"
            targetClass.platform.isCommon() -> "this::class"
            else -> defaultExpression
        }
    }

    private fun isNestedArray(variable: VariableDescriptor) =
        KotlinBuiltIns.isArrayOrPrimitiveArray(variable.builtIns.getArrayElementType(variable.type))

    private fun KtElement.canUseArrayContentFunctions() =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_1

    private fun generateArraysEqualsCall(
        variable: VariableDescriptor,
        canUseContentFunctions: Boolean,
        arg1: String,
        arg2: String
    ): String {
        return if (canUseContentFunctions) {
            val methodName = if (isNestedArray(variable)) "contentDeepEquals" else "contentEquals"
            "$arg1.$methodName($arg2)"
        } else {
            val methodName = if (isNestedArray(variable)) "deepEquals" else "equals"
            "java.util.Arrays.$methodName($arg1, $arg2)"
        }
    }

    private fun generateArrayHashCodeCall(
        variable: VariableDescriptor,
        canUseContentFunctions: Boolean,
        argument: String
    ): String {
        return if (canUseContentFunctions) {
            val methodName = if (isNestedArray(variable)) "contentDeepHashCode" else "contentHashCode"
            val dot = if (TypeUtils.isNullableType(variable.type)) "?." else "."
            "$argument$dot$methodName()"
        } else {
            val methodName = if (isNestedArray(variable)) "deepHashCode" else "hashCode"
            "java.util.Arrays.$methodName($argument)"
        }
    }

    private fun generateEquals(project: Project, info: Info, targetClass: KtClassOrObject): KtNamedFunction? {
        with(info) {
            if (!needEquals) return null

            val superEquals = classDescriptor.getSuperClassOrAny().findDeclaredEquals(true)!!
            val equalsFun = generateFunctionSkeleton(superEquals, targetClass)

            val paramName = equalsFun.valueParameters.first().name!!.quoteIfNeeded()
            var typeForCast = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classDescriptor)
            val typeParams = classDescriptor.declaredTypeParameters
            if (typeParams.isNotEmpty()) {
                typeForCast += typeParams.joinToString(prefix = "<", postfix = ">") { "*" }
            }

            val useIsCheck = CodeInsightSettings.getInstance().USE_INSTANCEOF_ON_EQUALS_PARAMETER
            val isNotInstanceCondition = if (useIsCheck) {
                "$paramName !is $typeForCast"
            } else {
                generateClassLiteralsNotEqual(paramName, targetClass)
            }
            val bodyText = buildString {
                append("if (this === $paramName) return true\n")
                append("if ($isNotInstanceCondition) return false\n")

                val builtIns = superEquals.builtIns
                if (!builtIns.isMemberOfAny(superEquals)) {
                    append("if (!super.equals($paramName)) return false\n")
                }

                if (variablesForEquals.isNotEmpty()) {
                    if (!useIsCheck) {
                        append("\n$paramName as $typeForCast\n")
                    }

                    append('\n')

                    variablesForEquals.forEach {
                        val isNullable = TypeUtils.isNullableType(it.type)
                        val isArray = KotlinBuiltIns.isArrayOrPrimitiveArray(it.type)
                        val canUseArrayContentFunctions = targetClass.canUseArrayContentFunctions()
                        val propName =
                            (DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) as PsiNameIdentifierOwner).nameIdentifier!!.text
                        val notEquals = when {
                            isArray -> {
                                "!${generateArraysEqualsCall(it, canUseArrayContentFunctions, propName, "$paramName.$propName")}"
                            }
                            else -> {
                                "$propName != $paramName.$propName"
                            }
                        }
                        val equalsCheck = "if ($notEquals) return false\n"
                        if (isArray && isNullable && canUseArrayContentFunctions) {
                            append("if ($propName != null) {\n")
                            append("if ($paramName.$propName == null) return false\n")
                            append(equalsCheck)
                            append("} else if ($paramName.$propName != null) return false\n")
                        } else {
                            append(equalsCheck)
                        }
                    }

                    append('\n')
                }

                append("return true")
            }

            equalsFun.replaceBody { KtPsiFactory(project).createBlock(bodyText) }

            return equalsFun
        }
    }

    private fun generateHashCode(project: Project, info: Info, targetClass: KtClassOrObject): KtNamedFunction? {
        fun VariableDescriptor.genVariableHashCode(parenthesesNeeded: Boolean): String {
            val ref = (DescriptorToSourceUtilsIde.getAnyDeclaration(project, this) as PsiNameIdentifierOwner).nameIdentifier!!.text
            val isNullable = TypeUtils.isNullableType(type)

            val builtIns = builtIns

            val typeClass = type.constructor.declarationDescriptor
            var text = when {
                typeClass == builtIns.byte || typeClass == builtIns.short || typeClass == builtIns.int ->
                    ref
                KotlinBuiltIns.isArrayOrPrimitiveArray(type) -> {
                    val canUseArrayContentFunctions = targetClass.canUseArrayContentFunctions()
                    val shouldWrapInLet = isNullable && !canUseArrayContentFunctions
                    val hashCodeArg = if (shouldWrapInLet) "it" else ref
                    val hashCodeCall = generateArrayHashCodeCall(this, canUseArrayContentFunctions, hashCodeArg)
                    if (shouldWrapInLet) "$ref?.let { $hashCodeCall }" else hashCodeCall
                }
                else ->
                    if (isNullable) "$ref?.hashCode()" else "$ref.hashCode()"
            }
            if (isNullable) {
                text += " ?: 0"
                if (parenthesesNeeded) {
                    text = "($text)"
                }
            }

            return text
        }

        with(info) {
            if (!needHashCode) return null

            val superHashCode = classDescriptor.getSuperClassOrAny().findDeclaredHashCode(true)!!
            val hashCodeFun = generateFunctionSkeleton(superHashCode, targetClass)
            val builtins = superHashCode.builtIns

            val propertyIterator = variablesForHashCode.iterator()
            val initialValue = when {
                !builtins.isMemberOfAny(superHashCode) -> "super.hashCode()"
                propertyIterator.hasNext() -> propertyIterator.next().genVariableHashCode(false)
                else -> generateClassLiteral(targetClass) + ".hashCode()"
            }

            val bodyText = if (propertyIterator.hasNext()) {
                val validator = CollectingNameValidator(variablesForEquals.map { it.name.asString().quoteIfNeeded() })
                val resultVarName = KotlinNameSuggester.suggestNameByName("result", validator)
                StringBuilder().apply {
                    append("var $resultVarName = $initialValue\n")
                    propertyIterator.forEach { append("$resultVarName = 31 * $resultVarName + ${it.genVariableHashCode(true)}\n") }
                    append("return $resultVarName")
                }.toString()
            } else "return $initialValue"

            hashCodeFun.replaceBody { KtPsiFactory(project).createBlock(bodyText) }

            return hashCodeFun
        }
    }

    override fun generateMembers(project: Project, editor: Editor?, info: Info): List<KtDeclaration> {
        val targetClass = info.classDescriptor.source.getPsi() as KtClass
        val prototypes = ArrayList<KtDeclaration>(2)
            .apply {
                addIfNotNull(generateEquals(project, info, targetClass))
                addIfNotNull(generateHashCode(project, info, targetClass))
            }
        val anchor = with(targetClass.declarations) { lastIsInstanceOrNull<KtNamedFunction>() ?: lastOrNull() }
        return insertMembersAfter(editor, targetClass, prototypes, anchor)
    }
}
