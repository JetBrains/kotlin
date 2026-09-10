/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightReferenceListBuilder
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.annotations.EmptyAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.MethodAdditionalAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.isTypeParameter
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterForMappedJavaCollectionStubMethod
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterListForMappedJavaCollectionStubMethod
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
    /**
     * Maps the type parameters of the Java collection class declaring [javaMethod] to the type arguments
     * of the corresponding Kotlin collection supertype of [containingClass].
     *
     * For example, for `class StringList : List<String>`, it maps `E` of `java.util.List` to `String`.
     * Type parameters of [javaMethod] itself are handled by [methodSubstitutor].
     */
    private val classSubstitutor: PsiSubstitutor,
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

    private val _parameterList by lazyPub {
        SymbolLightParameterList(parent = this) { builder ->
            javaMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val typeFromJava = paramFromJava.type
                val providedType = providedSignature?.parameterTypes?.get(index)
                val candidateType = providedType ?: substituteType(typeFromJava)
                val shouldTryToUnbox = providedType != null ||
                        (typeFromJava.isJavaLangObject() && substituteObjectWith == candidateType) ||
                        typeFromJava.isTypeParameter()
                val type = if (shouldTryToUnbox) candidateType.unboxedOrSelf() else candidateType

                builder.addParameter(
                    SymbolLightParameterForMappedJavaCollectionStubMethod(
                        javaParameter = paramFromJava,
                        type = type,
                        containingMethod = this,
                    )
                )
            }
        }
    }

    override fun getParameterList(): PsiParameterList = _parameterList

    private fun PsiType.isJavaLangObject(): Boolean =
        this is PsiClassType && this.canonicalText == CommonClassNames.JAVA_LANG_OBJECT

    private fun PsiType.unboxedOrSelf(): PsiType =
        PsiPrimitiveType.getUnboxedType(this)?.annotate(TypeAnnotationProvider.EMPTY) ?: this

    /**
     * [classSubstitutor] extended with the mapping from the type parameters of [javaMethod] to the own type parameters of this method,
     * so substituted types refer to the type parameters owned by the stub rather than by the Java declaration.
     */
    internal val methodSubstitutor: PsiSubstitutor by lazyPub {
        val ownTypeParameters = typeParameters
        if (ownTypeParameters.isEmpty()) {
            classSubstitutor
        } else {
            javaMethod.typeParameters.zip(ownTypeParameters).fold(classSubstitutor) { acc, [javaTypeParameter, ownTypeParameter] ->
                acc.put(javaTypeParameter, PsiTypesUtil.getClassType(ownTypeParameter))
            }
        }
    }

    private fun substituteType(psiType: PsiType): PsiType {
        val substituted = methodSubstitutor.substitute(psiType) ?: psiType
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
        val javaTypeParameters = javaMethod.typeParameters
        if (javaTypeParameters.isEmpty()) {
            null
        } else {
            SymbolLightTypeParameterListForMappedJavaCollectionStubMethod(owner = this, javaTypeParameters = javaTypeParameters)
        }
    }

    override fun hasTypeParameters(): Boolean = javaMethod.hasTypeParameters()

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun computeThrowsList(builder: LightReferenceListBuilder) {
        for (thrownType in javaMethod.throwsList.referencedTypes) {
            builder.addReference(methodSubstitutor.substitute(thrownType) as? PsiClassType ?: thrownType)
        }
    }

    override fun isOverride(): Boolean = true

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
