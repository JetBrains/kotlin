/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypeParameterList
import com.intellij.psi.SyntheticElement
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.impl.light.LightReferenceListBuilder
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import java.util.Objects

private class KotlinEnumSyntheticMethod(
    private val enumClass: KtExtensibleLightClass,
    private val kind: Kind
) : LightElement(enumClass.manager, enumClass.language), KtLightMethod, SyntheticElement {
    enum class Kind(val methodName: String) {
        VALUE_OF("valueOf"), VALUES("values"), ENTRIES("getEntries"),
    }

    private val returnType = run {
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val enumType = elementFactory.createType(enumClass).withNotNullAnnotation()

        when (kind) {
            Kind.VALUE_OF -> enumType
            Kind.VALUES -> enumType.createArrayType().withNotNullAnnotation()
            Kind.ENTRIES -> {
                val enumEntriesClass = JavaPsiFacade.getInstance(project).findClass(
                    /* qualifiedName = */ StandardClassIds.EnumEntries.asFqNameString(),
                    /* scope = */ resolveScope,
                )

                val type = if (enumEntriesClass != null) {
                    elementFactory.createType(enumEntriesClass, enumType)
                } else {
                    elementFactory.createTypeFromText(
                        /* text = */ "${StandardClassIds.EnumEntries.asFqNameString()}<${enumClass.qualifiedName}>",
                        /* context = */ enumClass,
                    )
                }

                type.withNotNullAnnotation()
            }
        }
    }

    private fun PsiType.withNotNullAnnotation(): PsiType = annotate { arrayOf(makeNotNullAnnotation(enumClass)) }

    private val parameterList = LightParameterListBuilder(manager, language).apply {
        if (kind == Kind.VALUE_OF) {
            val stringType = PsiType.getJavaLangString(manager, GlobalSearchScope.allScope(project)).withNotNullAnnotation()
            val valueParameter =
                object : LightParameter(
                    DEFAULT_VALUE_PARAMETER.identifier,
                    stringType,
                    this,
                    language,
                    false
                ), KtLightParameter {
                    override val method: KtLightMethod get() = this@KotlinEnumSyntheticMethod
                    override val kotlinOrigin: KtParameter? get() = null
                    override fun getParent(): PsiElement = this@KotlinEnumSyntheticMethod
                    override fun getContainingFile(): PsiFile = this@KotlinEnumSyntheticMethod.containingFile

                    override fun getText(): String = name
                    override fun getTextRange(): TextRange = TextRange.EMPTY_RANGE
                }

            addParameter(valueParameter)
        }
    }

    private val modifierList = object : LightModifierList(manager, language, PsiModifier.PUBLIC, PsiModifier.STATIC) {
        override fun getParent() = this@KotlinEnumSyntheticMethod

        private val annotations = arrayOf(makeNotNullAnnotation(enumClass))

        override fun findAnnotation(fqn: String): PsiAnnotation? = annotations.firstOrNull { it.hasQualifiedName(fqn) }
        override fun getAnnotations(): Array<PsiAnnotation> = annotations.clone()
    }

    override fun getTextOffset(): Int = enumClass.textOffset
    override fun toString(): String = enumClass.toString()

    override fun equals(other: Any?): Boolean {
        return this === other || (other is KotlinEnumSyntheticMethod && enumClass == other.enumClass && kind == other.kind)
    }

    override fun hashCode() = Objects.hash(enumClass, kind)

    override fun isDeprecated(): Boolean = false
    override fun getDocComment(): PsiDocComment? = null
    override fun getReturnType(): PsiType = returnType
    override fun getReturnTypeElement(): PsiTypeElement? = null
    override fun getParameterList(): PsiParameterList = parameterList

    override fun getThrowsList(): PsiReferenceList =
        LightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST).apply {
            if (kind == Kind.VALUE_OF) {
                addReference(java.lang.IllegalArgumentException::class.qualifiedName)
                addReference(java.lang.NullPointerException::class.qualifiedName)
            }
        }

    override fun getParent(): PsiElement = enumClass
    override fun getContainingClass(): KtExtensibleLightClass = enumClass
    override fun getContainingFile(): PsiFile = enumClass.containingFile

    override fun getBody(): PsiCodeBlock? = null
    override fun isConstructor(): Boolean = false
    override fun isVarArgs(): Boolean = false
    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature = MethodSignatureBackedByPsiMethod.create(this, substitutor)
    override fun getNameIdentifier(): PsiIdentifier = LightIdentifier(manager, name)
    override fun getName() = kind.methodName

    override fun findSuperMethods(): Array<PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)
    override fun findSuperMethods(checkAccess: Boolean): Array<PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)
    override fun findSuperMethods(parentClass: PsiClass): Array<PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> {
        return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun findDeepestSuperMethod(): PsiMethod? = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getModifierList(): PsiModifierList = modifierList
    override fun hasModifierProperty(name: String) = name == PsiModifier.PUBLIC || name == PsiModifier.STATIC
    override fun setName(name: String): PsiElement = throw IncorrectOperationException()
    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)
    override fun getDefaultValue(): PsiAnnotationMemberValue? = null

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override val isMangled: Boolean get() = false
    override val lightMemberOrigin: LightMemberOrigin? get() = null
    override val kotlinOrigin: KtDeclaration? get() = null

    override fun getText(): String = ""
    override fun getTextRange(): TextRange = TextRange.EMPTY_RANGE

    private companion object {
        private val NOT_NULL_ANNOTATION_QUALIFIER: String = "@" + NotNull::class.qualifiedName

        private fun makeNotNullAnnotation(context: PsiClass): PsiAnnotation {
            return PsiElementFactory.getInstance(context.project).createAnnotationFromText(
                NOT_NULL_ANNOTATION_QUALIFIER,
                context,
            )
        }
    }
}

fun getEnumEntriesPsiMethod(enumClass: KtExtensibleLightClass): PsiMethod =
    KotlinEnumSyntheticMethod(enumClass, KotlinEnumSyntheticMethod.Kind.ENTRIES)

fun getEnumValueOfPsiMethod(enumClass: KtExtensibleLightClass): PsiMethod =
    KotlinEnumSyntheticMethod(enumClass, KotlinEnumSyntheticMethod.Kind.VALUE_OF)

fun getEnumValuesPsiMethod(enumClass: KtExtensibleLightClass): PsiMethod =
    KotlinEnumSyntheticMethod(enumClass, KotlinEnumSyntheticMethod.Kind.VALUES)