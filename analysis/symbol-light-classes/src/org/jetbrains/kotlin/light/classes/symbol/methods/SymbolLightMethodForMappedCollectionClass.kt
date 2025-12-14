/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.impl.light.LightTypeParameterBuilder
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.annotations.ComputeAllAtOnceAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.isTypeParameter
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtDeclaration
import javax.swing.Icon

internal data class MethodSignature(val parameterTypes: List<PsiType>, val returnType: PsiType)

internal class SymbolLightMethodForMappedCollectionClass(
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
        object : LightParameterListBuilder(manager, KotlinLanguage.INSTANCE) {
            override fun getParent(): PsiElement = this@SymbolLightMethodForMappedCollectionClass
            override fun getElementIcon(flags: Int): Icon? = null
        }.apply {
            javaMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val typeFromJava = paramFromJava.type
                val providedType = providedSignature?.parameterTypes?.get(index)
                val candidateType = providedType ?: substituteType(typeFromJava)
                val shouldTryToUnbox = typeFromJava.isTypeParameter() ||
                        providedType != null ||
                        (typeFromJava.isJavaLangObject() && substituteObjectWith == candidateType)
                val type = if (shouldTryToUnbox) candidateType.unboxedOrSelf() else candidateType

                addParameter(
                    object : LightParameter(
                        paramFromJava.name,
                        type,
                        this@SymbolLightMethodForMappedCollectionClass,
                        KotlinLanguage.INSTANCE,
                        paramFromJava.isVarArgs
                    ) {
                        override fun getParent(): PsiElement = this@apply
                    }
                )
            }
        }
    }

    private fun PsiType.isJavaLangObject(): Boolean =
        this is PsiClassType && this.canonicalText == CommonClassNames.JAVA_LANG_OBJECT

    private fun PsiType.unboxedOrSelf(): PsiType =
        PsiPrimitiveType.getUnboxedType(this)?.annotate(TypeAnnotationProvider.EMPTY) ?: this

    private fun substituteType(psiType: PsiType): PsiType {
        val substituted = substitutor.substitute(psiType) ?: psiType
        return if (substituted.isJavaLangObject() && substituteObjectWith != null) {
            substituteObjectWith
        } else {
            substituted
        }
    }

    override fun getName(): String = name

    override fun getReturnType(): PsiType? =
        providedSignature?.returnType ?: javaMethod.returnType?.let { substituteType(it) }

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        if (javaMethod.typeParameters.isEmpty()) {
            null
        } else {
            object : LightTypeParameterListBuilder(manager, KotlinLanguage.INSTANCE) {
                override fun getParent(): PsiElement = this@SymbolLightMethodForMappedCollectionClass
                override fun getElementIcon(flags: Int): Icon? = null
            }.apply {
                javaMethod.typeParameters.forEach { typeParam ->
                    val builder = object : LightTypeParameterBuilder(
                        typeParam.name ?: "",
                        this@SymbolLightMethodForMappedCollectionClass,
                        typeParam.index
                    ) {
                        override fun getParent(): PsiElement = this@apply
                        override fun getElementIcon(flags: Int): Icon? = null
                    }
                    addParameter(builder)
                }
            }
        }
    }

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getThrowsList(): PsiReferenceList = javaMethod.throwsList

    override fun isOverride(): Boolean = true

    override fun hasTypeParameters(): Boolean = javaMethod.hasTypeParameters()

    override fun isVarArgs(): Boolean = javaMethod.isVarArgs

    override fun isConstructor(): Boolean = false

    private val identifier: LightIdentifier by lazyPub { LightIdentifier(manager, name) }

    override fun getNameIdentifier(): LightIdentifier = identifier

    override fun getDocComment(): PsiDocComment? = javaMethod.docComment

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox { modifier ->
                mapOf(modifier to hasModifierProperty(modifier))
            },
            annotationsBox = ComputeAllAtOnceAnnotationsBox { parent ->
                listOf(SymbolLightSimpleAnnotation(fqName = CommonClassNames.JAVA_LANG_OVERRIDE, parent = parent))
            },
        )
    }

    override fun isDeprecated(): Boolean = javaMethod.isDeprecated

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethodForMappedCollectionClass) return false

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