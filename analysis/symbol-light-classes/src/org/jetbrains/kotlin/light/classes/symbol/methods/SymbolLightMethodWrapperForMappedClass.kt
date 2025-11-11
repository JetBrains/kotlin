/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.CommonClassNames
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
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
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import javax.swing.Icon

internal data class MethodSignature(val parameterTypes: List<PsiType>, val returnType: PsiType)

internal class SymbolLightMethodWrapperForMappedClass(
    private val containingClass: SymbolLightClassForClassOrObject,
    private val javaMethod: PsiMethod,
    private val substitutor: PsiSubstitutor,
    private val lightMemberOrigin: LightMemberOrigin?,
    private val name: String,
    private val isFinal: Boolean,
    private val hasImplementation: Boolean,
    private val substituteObjectWith: PsiType?,
    private val providedSignature: MethodSignature?,
) : PsiMethod, KtLightElementBase(containingClass) {

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

    override val kotlinOrigin: KtElement? get() = lightMemberOrigin?.originalElement

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
                        this@SymbolLightMethodWrapperForMappedClass,
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

    override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)

    override fun findSuperMethods(parentClass: PsiClass): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun findSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean) =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    @Deprecated("Deprecated in Java")
    override fun findDeepestSuperMethod(): PsiMethod? = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)
    override fun findDeepestSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)

    override fun getHierarchicalMethodSignature(): HierarchicalMethodSignature = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)
    override fun getSignature(substitutor: PsiSubstitutor): MethodSignatureBackedByPsiMethod =
        MethodSignatureBackedByPsiMethod.create(this, substitutor)

    override fun getReturnTypeElement(): PsiTypeElement? = null

    @Suppress("UnstableApiUsage")
    override fun getContainingClass(): PsiClass = containingClass
    override fun getThrowsList(): PsiReferenceList = javaMethod.throwsList
    override fun hasTypeParameters(): Boolean = javaMethod.hasTypeParameters()
    override fun isVarArgs(): Boolean = javaMethod.isVarArgs
    override fun isConstructor(): Boolean = false

    private val identifier: LightIdentifier by lazyPub { LightIdentifier(manager, name) }

    override fun getNameIdentifier(): LightIdentifier = identifier
    override fun getDocComment(): PsiDocComment? = javaMethod.docComment
    override fun getModifierList(): PsiModifierList = javaMethod.modifierList
    override fun getBody(): PsiCodeBlock? = null
    override fun isDeprecated(): Boolean = javaMethod.isDeprecated
    override fun setName(name: String): Nothing = cannotModify()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethodWrapperForMappedClass) return false

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

    // TODO remove PSI access
    override fun toString(): String {
        return "$javaClass:$name${parameterList.parameters.map { it.type }.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
    }
}