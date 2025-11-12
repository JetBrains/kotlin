/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.psi.KtDeclaration
import javax.swing.Icon

internal data class MethodSignature(val parameterTypes: List<PsiType>, val returnType: PsiType)

internal class SymbolLightMethodForMappedClass(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassForClassOrObject,
    private val javaMethod: PsiMethod,
    private val substitutor: PsiSubstitutor,
    private val name: String,
    private val isFinal: Boolean,
    private val hasImplementation: Boolean,
    private val substituteObjectWith: PsiType?,
    private val providedSignature: MethodSignature?,
) : SymbolLightMethodBase(lightMemberOrigin, containingClass, methodIndex = METHOD_INDEX_BASE, isJvmExposedBoxed = false) {

    init {
        if (!hasImplementation && isFinal) {
            error("Can't be final without an implementation")
        }
    }

    override fun getPresentation(): ItemPresentation? =
        kotlinOrigin?.presentation ?: javaMethod.presentation

    override fun getNavigationElement(): PsiElement =
        kotlinOrigin?.navigationElement ?: javaMethod.navigationElement

    override fun getIcon(flags: Int): Icon? =
        kotlinOrigin?.getIcon(flags) ?: javaMethod.getIcon(flags)

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun hasModifierProperty(name: String): Boolean = when (name) {
        PsiModifier.ABSTRACT -> !hasImplementation
        PsiModifier.FINAL -> isFinal
        else -> javaMethod.hasModifierProperty(name)
    }

    override fun getParameterList(): PsiParameterList = cachedValue {
        LightParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            javaMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val type = providedSignature?.parameterTypes?.get(index) ?: substituteType(paramFromJava.type)
                addParameter(
                    LightParameter(
                        paramFromJava.name,
                        type,
                        this@SymbolLightMethodForMappedClass,
                        KotlinLanguage.INSTANCE,
                        paramFromJava.isVarArgs
                    )
                )
            }
        }
    }

    private fun substituteType(psiType: PsiType): PsiType {
        fun isJavaLangObject(type: PsiType?): Boolean =
            type is PsiClassType && type.canonicalText == CommonClassNames.JAVA_LANG_OBJECT

        val substituted = substitutor.substitute(psiType) ?: psiType
        return if (isJavaLangObject(substituted) && substituteObjectWith != null) {
            substituteObjectWith
        } else {
            substituted
        }
    }

    override fun getName(): String = name

    override fun getReturnType(): PsiType? =
        providedSignature?.returnType ?: javaMethod.returnType?.let { substituteType(it) }

    override fun getTypeParameters(): Array<PsiTypeParameter> = javaMethod.typeParameters

    override fun getTypeParameterList(): PsiTypeParameterList? = javaMethod.typeParameterList

    override fun getThrowsList(): PsiReferenceList = javaMethod.throwsList

    override fun isOverride(): Boolean = true

    override fun hasTypeParameters(): Boolean = javaMethod.hasTypeParameters()

    override fun isVarArgs(): Boolean = javaMethod.isVarArgs

    override fun isConstructor(): Boolean = false

    private val identifier: LightIdentifier by lazyPub { LightIdentifier(manager, name) }

    override fun getNameIdentifier(): LightIdentifier = identifier

    override fun getDocComment(): PsiDocComment? = javaMethod.docComment

    private val _modifierList: PsiModifierList by lazyPub {
        object : LightModifierList(manager, language) {
            override fun getParent(): PsiElement = this@SymbolLightMethodForMappedClass

            private val overrideAnnotation by lazy {
                SymbolLightSimpleAnnotation(fqName = CommonClassNames.JAVA_LANG_OVERRIDE, parent = this)
            }

            private val allAnnotations: Array<PsiAnnotation> by lazy { arrayOf(overrideAnnotation) }

            override fun hasModifierProperty(name: String): Boolean =
                this@SymbolLightMethodForMappedClass.hasModifierProperty(name)

            override fun hasExplicitModifier(name: String): Boolean = hasModifierProperty(name)

            override fun getAnnotations(): Array<PsiAnnotation> = allAnnotations

            override fun findAnnotation(qualifiedName: String): PsiAnnotation? =
                if (qualifiedName == CommonClassNames.JAVA_LANG_OVERRIDE) overrideAnnotation else null

            override fun hasAnnotation(qualifiedName: String): Boolean =
                qualifiedName == CommonClassNames.JAVA_LANG_OVERRIDE
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isDeprecated(): Boolean = javaMethod.isDeprecated

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethodForMappedClass) return false

        if (name != other.name) return false
        if (isFinal != other.isFinal) return false
        if (hasImplementation != other.hasImplementation) return false
        if (providedSignature != other.providedSignature) return false
        if (returnType != other.returnType) return false
        if (substitutor != other.substitutor) return false
        if (substituteObjectWith != other.substituteObjectWith) return false
        if (javaMethod != other.javaMethod) return false
        if (containingClass != other.containingClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isFinal.hashCode()
        result = 31 * result + hasImplementation.hashCode()
        result = 31 * result + (providedSignature?.hashCode() ?: 0)
        result = 31 * result + (returnType?.hashCode() ?: 0)
        result = 31 * result + substitutor.hashCode()
        result = 31 * result + (substituteObjectWith?.hashCode() ?: 0)
        result = 31 * result + javaMethod.hashCode()
        result = 31 * result + containingClass.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName.orEmpty()}:$name"
}