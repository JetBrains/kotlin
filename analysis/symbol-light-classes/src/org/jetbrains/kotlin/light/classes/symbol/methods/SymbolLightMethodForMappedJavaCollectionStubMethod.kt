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
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.annotations.EmptyAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.MethodAdditionalAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.isTypeParameter
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import javax.swing.Icon

internal data class MethodSignature(val parameterTypes: List<PsiType>, val returnType: PsiType)

/**
 * Represents a stub method generated for a mapped Java collection in Kotlin's custom collection type.
 *
 * #### Example
 *
 * ```
 * abstract class CCollection : Collection<String>
 * ```
 *
 * In Kotlin, `CCollection` is a read-only collection. However, from the Java (and JVM) point of view, it is a subclass of
 * `java.util.Collection`. So, it needs to override all methods that are in (mutable) `java.util.Collection` but not in Kotlin's
 * (read-only) Collection. For example, we generate two stubs for the method `add`:
 *
 * * add(Ljava/lang/String;)Z
 * * synthetic bridge add(Ljava/lang/Object;)Z
 *
 * These stubs are represented by the current class.
 */
internal class SymbolLightMethodForMappedJavaCollectionStubMethod(
    containingClass: SymbolLightClassForClassOrObject,
    private val javaMethod: PsiMethod,
    private val substitutor: PsiSubstitutor,
    private val name: String,
    private val isFinal: Boolean,
    private val hasImplementation: Boolean,
    private val substituteObjectWith: PsiType?,
    private val providedSignature: MethodSignature?,
) : SymbolLightMethodBase(lightMemberOrigin = null, containingClass, methodIndex = METHOD_INDEX_BASE, isJvmExposedBoxed = false),
    SyntheticElement {

    init {
        if (!hasImplementation && isFinal) {
            error("Can't be final without an implementation")
        }
    }

    override fun getPresentation(): ItemPresentation? = javaMethod.presentation
    override fun getNavigationElement(): PsiElement = javaMethod.navigationElement
    override fun getIcon(flags: Int): Icon? = javaMethod.getIcon(flags)

    override fun getParameterList(): PsiParameterList = cachedValue {
        LightParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            javaMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val typeFromJava = paramFromJava.type
                val providedType = providedSignature?.parameterTypes?.get(index)
                val candidateType = providedType ?: substituteType(typeFromJava)
                val shouldTryToUnbox = providedType != null ||
                        (typeFromJava.isJavaLangObject() && substituteObjectWith == candidateType) ||
                        typeFromJava.isTypeParameter()
                val type = if (shouldTryToUnbox) candidateType.unboxedOrSelf() else candidateType

                addParameter(
                    LightParameter(
                        paramFromJava.name,
                        type,
                        this@SymbolLightMethodForMappedJavaCollectionStubMethod,
                        KotlinLanguage.INSTANCE,
                        paramFromJava.isVarArgs
                    )
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

    override fun getTypeParameters(): Array<PsiTypeParameter> = javaMethod.typeParameters

    override fun getTypeParameterList(): PsiTypeParameterList? = javaMethod.typeParameterList

    override fun getThrowsList(): PsiReferenceList = javaMethod.throwsList

    override fun isOverride(): Boolean = true

    override fun hasTypeParameters(): Boolean = javaMethod.hasTypeParameters()

    override fun isVarArgs(): Boolean = javaMethod.isVarArgs

    override fun isConstructor(): Boolean = false

    override fun getNameIdentifier(): LightIdentifier = LightIdentifier(manager, name)

    override fun getDocComment(): PsiDocComment? = javaMethod.docComment

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = EmptyAnnotationsProvider,
                additionalAnnotationsProvider = MethodAdditionalAnnotationsProvider,
            ),
        )
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in GranularModifiersBox.MODALITY_MODIFIERS -> {
            val modality = when {
                !hasImplementation -> PsiModifier.ABSTRACT
                isFinal -> PsiModifier.FINAL
                else -> null
            }
            GranularModifiersBox.MODALITY_MODIFIERS_MAP.with(modality)
        }

        PsiModifier.DEFAULT -> {
            // In the case of the overridden function 'Map.getOrDefault', the Kotlin override is not default, even though the original
            // Java method is. And in all other cases the mapped methods are not supposed to be default.
            mapOf(modifier to false)
        }

        else -> mapOf(modifier to javaMethod.hasModifierProperty(modifier))
    }

    override fun isDeprecated(): Boolean = javaMethod.isDeprecated

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethodForMappedJavaCollectionStubMethod) return false

        if (name != other.name) return false
        if (isFinal != other.isFinal) return false
        if (hasImplementation != other.hasImplementation) return false
        if (javaMethod != other.javaMethod) return false
        if (containingClass != other.containingClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isFinal.hashCode()
        result = 31 * result + hasImplementation.hashCode()
        result = 31 * result + javaMethod.hashCode()
        result = 31 * result + containingClass.hashCode()
        return result
    }

    override fun toString(): String = "${this::class.simpleName.orEmpty()}:$name"
}
