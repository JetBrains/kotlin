/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.CLASS_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.FUNCTION_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.INHERITANCE_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.INLINE_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.MEMBER_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.PARAMETER_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.PLATFORM_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.PROPERTY_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.VISIBILITY_MODIFIER

class Modifier(
    private val classModifiers: MutableList<ClassModifier> = mutableListOf(),
    private val memberModifiers: MutableList<MemberModifier> = mutableListOf(),
    private val visibilityModifiers: MutableList<VisibilityModifier> = mutableListOf(),
    private val functionModifiers: MutableList<FunctionModifier> = mutableListOf(),
    private var propertyModifier: PropertyModifier? = null,
    private val inheritanceModifiers: MutableList<InheritanceModifier> = mutableListOf(),
    private val parameterModifiers: MutableList<ParameterModifier> = mutableListOf(),
    private val platformModifiers: MutableList<PlatformModifier> = mutableListOf()
) {
    val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    fun addModifier(modifier: LighterASTNode, isInClass: Boolean = false) {
        val tokenType = modifier.tokenType
        when {
            INLINE_MODIFIER.contains(tokenType) -> {
                if (isInClass)
                    this.classModifiers += ClassModifier.valueOf(modifier.toString().toUpperCase())
                else
                    this.functionModifiers += FunctionModifier.valueOf(modifier.toString().toUpperCase())
            }
            CLASS_MODIFIER.contains(tokenType) -> this.classModifiers += ClassModifier.valueOf(modifier.toString().toUpperCase())
            MEMBER_MODIFIER.contains(tokenType) -> this.memberModifiers += MemberModifier.valueOf(modifier.toString().toUpperCase())
            VISIBILITY_MODIFIER.contains(tokenType) -> this.visibilityModifiers +=
                VisibilityModifier.valueOf(modifier.toString().toUpperCase())
            FUNCTION_MODIFIER.contains(tokenType) -> this.functionModifiers += FunctionModifier.valueOf(modifier.toString().toUpperCase())
            PROPERTY_MODIFIER.contains(tokenType) -> this.propertyModifier = PropertyModifier.valueOf(modifier.toString().toUpperCase())
            INHERITANCE_MODIFIER.contains(tokenType) -> this.inheritanceModifiers +=
                InheritanceModifier.valueOf(modifier.toString().toUpperCase())
            PARAMETER_MODIFIER.contains(tokenType) -> this.parameterModifiers += ParameterModifier.valueOf(modifier.toString().toUpperCase())
            PLATFORM_MODIFIER.contains(tokenType) -> this.platformModifiers += PlatformModifier.valueOf(modifier.toString().toUpperCase())
        }
    }

    fun isEnum(): Boolean {
        return classModifiers.contains(ClassModifier.ENUM)
    }

    fun isAnnotation(): Boolean {
        return classModifiers.contains(ClassModifier.ANNOTATION)
    }

    fun isDataClass(): Boolean {
        return classModifiers.contains(ClassModifier.DATA)
    }

    fun isInlineClass(): Boolean {
        return classModifiers.contains(ClassModifier.INLINE)
    }

    fun isInner(): Boolean {
        return classModifiers.contains(ClassModifier.INNER)
    }

    fun isCompanion(): Boolean {
        return classModifiers.contains(ClassModifier.COMPANION)
    }

    fun isFunctionalInterface(): Boolean {
        return classModifiers.contains(ClassModifier.FUN)
    }

    fun hasOverride(): Boolean {
        return memberModifiers.contains(MemberModifier.OVERRIDE)
    }

    fun hasLateinit(): Boolean {
        return memberModifiers.contains(MemberModifier.LATEINIT)
    }

    fun getVisibility(): Visibility {
        return when {
            visibilityModifiers.contains(VisibilityModifier.PRIVATE) -> Visibilities.Private
            visibilityModifiers.contains(VisibilityModifier.PUBLIC) -> Visibilities.Public
            visibilityModifiers.contains(VisibilityModifier.PROTECTED) -> Visibilities.Protected
            visibilityModifiers.contains(VisibilityModifier.INTERNAL) -> Visibilities.Internal
            else -> Visibilities.Unknown
        }
    }

    fun hasTailrec(): Boolean {
        return functionModifiers.contains(FunctionModifier.TAILREC)
    }

    fun hasOperator(): Boolean {
        return functionModifiers.contains(FunctionModifier.OPERATOR)
    }

    fun hasInfix(): Boolean {
        return functionModifiers.contains(FunctionModifier.INFIX)
    }

    fun hasInline(): Boolean {
        return functionModifiers.contains(FunctionModifier.INLINE)
    }

    fun hasExternal(): Boolean {
        return functionModifiers.contains(FunctionModifier.EXTERNAL)
    }

    fun hasSuspend(): Boolean {
        return functionModifiers.contains(FunctionModifier.SUSPEND)
    }

    fun isConst(): Boolean {
        return propertyModifier == PropertyModifier.CONST
    }

    fun hasModality(modality: Modality): Boolean {
        return when {
            modality == Modality.FINAL && inheritanceModifiers.contains(InheritanceModifier.FINAL) -> true
            modality == Modality.SEALED && inheritanceModifiers.contains(InheritanceModifier.SEALED) -> true
            modality == Modality.ABSTRACT && inheritanceModifiers.contains(InheritanceModifier.ABSTRACT) -> true
            modality == Modality.OPEN && inheritanceModifiers.contains(InheritanceModifier.OPEN) -> true
            else -> false
        }
    }

    fun getModality(): Modality? {
        return when {
            inheritanceModifiers.contains(InheritanceModifier.FINAL) -> Modality.FINAL
            inheritanceModifiers.contains(InheritanceModifier.SEALED) -> Modality.SEALED
            inheritanceModifiers.contains(InheritanceModifier.ABSTRACT) -> Modality.ABSTRACT
            inheritanceModifiers.contains(InheritanceModifier.OPEN) -> Modality.OPEN
            else -> null
        }
    }

    fun hasVararg(): Boolean {
        return parameterModifiers.contains(ParameterModifier.VARARG)
    }

    fun hasNoinline(): Boolean {
        return parameterModifiers.contains(ParameterModifier.NOINLINE)
    }

    fun hasCrossinline(): Boolean {
        return parameterModifiers.contains(ParameterModifier.CROSSINLINE)
    }

    fun hasExpect(): Boolean {
        return platformModifiers.contains(PlatformModifier.EXPECT) || platformModifiers.contains(PlatformModifier.HEADER)
    }

    fun hasActual(): Boolean {
        return platformModifiers.contains(PlatformModifier.ACTUAL) || platformModifiers.contains(PlatformModifier.IMPL)
    }
}
