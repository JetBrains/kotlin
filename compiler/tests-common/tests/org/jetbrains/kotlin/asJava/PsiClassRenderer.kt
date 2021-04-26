/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import com.intellij.psi.util.MethodSignature
import org.jetbrains.kotlin.asJava.elements.KtLightNullabilityAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightPsiArrayInitializerMemberValue
import org.jetbrains.kotlin.asJava.elements.KtLightPsiLiteral
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

fun PsiClass.renderClass() = PsiClassRenderer.renderClass(this)


class PsiClassRenderer private constructor(
    private val renderInner: Boolean,
    private val membersFilter: MembersFilter
) {

    interface MembersFilter {
        fun includeEnumConstant(psiEnumConstant: PsiEnumConstant): Boolean = true
        fun includeField(psiField: PsiField): Boolean = true
        fun includeMethod(psiMethod: PsiMethod): Boolean = true
        fun includeClass(psiClass: PsiClass): Boolean = true

        companion object {
            val DEFAULT = object : MembersFilter {}
        }
    }

    companion object {
        var extendedTypeRenderer = false

        fun renderClass(
            psiClass: PsiClass,
            renderInner: Boolean = false,
            membersFilter: MembersFilter = MembersFilter.DEFAULT
        ): String =
            PsiClassRenderer(renderInner, membersFilter).renderClass(psiClass)
    }

    private fun renderClass(psiClass: PsiClass): String =
        buildString {
            val classWord = when {
                psiClass.isAnnotationType -> "@interface"
                psiClass.isInterface -> "interface"
                psiClass.isEnum -> "enum"
                else -> "class"
            }

            append(psiClass.renderModifiers())
            append("$classWord ")
            append("${psiClass.name} /* ${psiClass.qualifiedName}*/")
            append(psiClass.typeParameters.renderTypeParams())
            append(psiClass.extendsList.renderRefList("extends"))
            append(psiClass.implementsList.renderRefList("implements"))
            appendLine(" {")

            if (psiClass.isEnum) {
                append(
                    psiClass.fields
                        .filterIsInstance<PsiEnumConstant>()
                        .filter { membersFilter.includeEnumConstant(it) }
                        .joinToString(",\n") { it.renderEnumConstant() }.prependDefaultIndent()
                )
                append(";\n\n")
            }

            append(psiClass.renderMembers())

            append("}")
        }

    private fun PsiType.renderType() = StringBuffer().also { renderType(it) }.toString()
    private fun PsiType.renderType(sb: StringBuffer) {
        if (extendedTypeRenderer && annotations.isNotEmpty()) {
            sb.append(annotations.joinToString(" ", postfix = " ") { it.renderAnnotation() })
        }
        when (this) {
            is PsiClassType -> {
                sb.append(PsiNameHelper.getQualifiedClassName(canonicalText, false))
                if (parameterCount > 0) {
                    sb.append("<")
                    parameters.forEachIndexed { index, type ->
                        type.renderType(sb)
                        if (index < parameterCount - 1) sb.append(", ")
                    }
                    sb.append(">")
                }
            }
            is PsiEllipsisType -> {
                componentType.renderType(sb)
                sb.append("...")
            }
            is PsiArrayType -> {
                componentType.renderType(sb)
                sb.append("[]")
            }
            else -> {
                sb.append(canonicalText)
            }
        }
    }


    private fun PsiReferenceList?.renderRefList(keyword: String, sortReferences: Boolean = true): String {
        if (this == null) return ""

        val references = referencedTypes
        if (references.isEmpty()) return ""

        val referencesTypes = references.map { it.renderType() }.toTypedArray()

        if (sortReferences) referencesTypes.sort()

        return " " + keyword + " " + referencesTypes.joinToString()
    }

    private fun PsiVariable.renderVar(): String {
        var result = this.renderModifiers(type) + type.renderType() + " " + name
        if (this is PsiParameter && this.isVarArgs) {
            result += " /* vararg */"
        }

        if (hasInitializer()) {
            result += " = ${initializer?.text} /* initializer type: ${initializer?.type?.renderType()} */"
        }

        computeConstantValue()?.let { result += " /* constant value $it */" }

        return result
    }

    private fun Array<PsiTypeParameter>.renderTypeParams() =
        if (isEmpty()) ""
        else "<" + joinToString {
            val bounds =
                if (it.extendsListTypes.isNotEmpty())
                    " extends " + it.extendsListTypes.joinToString(" & ", transform = { it.renderType() })
                else ""
            it.name!! + bounds
        } + "> "

    private fun KtLightPsiLiteral.renderKtLightPsiLiteral(): String =
        (value as? Pair<*, *>)?.let {
            val classId = it.first as? ClassId
            val name = it.second as? Name
            if (classId != null && name != null) "${classId.asSingleFqName()}.${name.asString()}" else null
        } ?: text

    private fun PsiAnnotationMemberValue.renderAnnotationMemberValue(): String = when (this) {
        is KtLightPsiArrayInitializerMemberValue -> "{${initializers.joinToString { it.renderAnnotationMemberValue() }}}"
        is PsiAnnotation -> renderAnnotation()
        is KtLightPsiLiteral -> renderKtLightPsiLiteral()
        else -> text
    }

    private fun PsiMethod.renderMethod() =
        renderModifiers(returnType) +
                (if (isVarArgs) "/* vararg */ " else "") +
                typeParameters.renderTypeParams() +
                (returnType?.renderType() ?: "") + " " +
                name +
                "(" + parameterList.parameters.joinToString { it.renderModifiers(it.type) + it.type.renderType() } + ")" +
                (this as? PsiAnnotationMethod)?.defaultValue?.let { " default " + it.renderAnnotationMemberValue() }.orEmpty() +
                throwsList.referencedTypes.let { thrownTypes ->
                    if (thrownTypes.isEmpty()) ""
                    else " throws " + thrownTypes.joinToString { it.renderType() }
                } +
                ";" +
                "// ${getSignature(PsiSubstitutor.EMPTY).renderSignature()}"

    private fun MethodSignature.renderSignature(): String {
        val typeParams = typeParameters.renderTypeParams()
        val paramTypes = parameterTypes.joinToString(prefix = "(", postfix = ")") { it.renderType() }
        val name = if (isConstructor) ".ctor" else name
        return "$typeParams $name$paramTypes"
    }

    private fun PsiEnumConstant.renderEnumConstant(): String {
        val initializingClass = initializingClass ?: return name

        return buildString {
            appendLine("$name {")
            append(initializingClass.renderMembers())
            append("}")
        }
    }

    private fun PsiClass.renderMembers(): String {
        return buildString {
            appendSorted(
                fields
                    .filterNot { it is PsiEnumConstant }
                    .filter { membersFilter.includeField(it) }
                    .map { it.renderVar().prependDefaultIndent() + ";\n\n" }
            )

            appendSorted(
                methods
                    .filter { membersFilter.includeMethod(it) }
                    .map { it.renderMethod().prependDefaultIndent() + "\n\n" }
            )

            appendSorted(
                innerClasses
                    .filter { membersFilter.includeClass(it) }
                    .map {
                        appendLine()
                        if (renderInner)
                            renderClass(it, renderInner)
                        else
                            "class ${it.name} ...\n\n".prependDefaultIndent()
                    }
            )
        }
    }

    private fun StringBuilder.appendSorted(list: List<String>) {
        append(list.sorted().joinToString(""))
    }

    private fun String.prependDefaultIndent() = prependIndent("  ")

    private fun PsiAnnotation.renderAnnotation(): String {

        if (qualifiedName == "kotlin.Metadata") return ""

        val renderedAttributes = parameterList.attributes.map {
            val attributeValue = it.value?.renderAnnotationMemberValue() ?: "?"

            val isAnnotationQualifiedName =
                (qualifiedName?.startsWith("java.lang.annotation.") == true || qualifiedName?.startsWith("kotlin.annotation.") == true)

            val name = if (it.name == null && isAnnotationQualifiedName) "value" else it.name


            if (name != null) "$name = $attributeValue" else attributeValue
        }

        val renderedAttributesString = renderedAttributes.joinToString()
        if (qualifiedName == null && renderedAttributesString.isEmpty()) {
            return ""
        }
        return "@$qualifiedName(${renderedAttributes.joinToString()})"
    }


    private fun PsiModifierListOwner.renderModifiers(typeIfApplicable: PsiType? = null): String {
        val annotationsBuffer = mutableListOf<String>()
        var nullableIsRendered = false
        var notNullIsRendered = false

        for (annotation in annotations) {
            if (annotation is KtLightNullabilityAnnotation<*> && skipRenderingNullability(typeIfApplicable)) {
                continue
            }

            if (annotation.qualifiedName == "org.jetbrains.annotations.Nullable") {
                if (nullableIsRendered) continue
                nullableIsRendered = true
            }

            if (annotation.qualifiedName == "org.jetbrains.annotations.NotNull") {
                if (notNullIsRendered) continue
                notNullIsRendered = true
            }

            val renderedAnnotation = annotation.renderAnnotation()
            if (renderedAnnotation.isNotEmpty()) {
                annotationsBuffer.add(
                    renderedAnnotation + (if (this is PsiParameter) " " else "\n")
                )
            }
        }
        annotationsBuffer.sort()

        val resultBuffer = StringBuffer(annotationsBuffer.joinToString(separator = ""))
        for (modifier in PsiModifier.MODIFIERS.filter(::hasModifierProperty)) {
            if (modifier == PsiModifier.DEFAULT) {
                resultBuffer.append(PsiModifier.ABSTRACT).append(" ")
            } else if (modifier != PsiModifier.FINAL || !(this is PsiClass && this.isEnum)) {
                resultBuffer.append(modifier).append(" ")
            }
        }
        return resultBuffer.toString()
    }

    private val NON_EXISTENT_QUALIFIED_CLASS_NAME = NON_EXISTENT_CLASS_NAME.replace("/", ".")

    private fun isPrimitiveOrNonExisting(typeIfApplicable: PsiType?): Boolean {
        if (typeIfApplicable is PsiPrimitiveType) return true
        if (typeIfApplicable?.getCanonicalText(false) == NON_EXISTENT_QUALIFIED_CLASS_NAME) return true

        return typeIfApplicable is PsiPrimitiveType
    }

    private fun PsiModifierListOwner.skipRenderingNullability(typeIfApplicable: PsiType?) =
        isPrimitiveOrNonExisting(typeIfApplicable)// || isPrivateOrParameterInPrivateMethod()

}