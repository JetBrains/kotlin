/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.CLASS_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.FUNCTION_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.INHERITANCE_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.MEMBER_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.PARAMETER_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.PLATFORM_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.PROPERTY_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.VISIBILITY_MODIFIER

class Modifier(
    session: FirSession,
    psi: PsiElement? = null,

    private var classModifier: ClassModifier? = null,
    private val memberModifiers: MutableList<MemberModifier> = mutableListOf(),
    private var visibilityModifier: VisibilityModifier = VisibilityModifier.UNKNOWN,
    private val functionModifiers: MutableList<FunctionModifier> = mutableListOf(),
    private var propertyModifier: PropertyModifier? = null,
    private var inheritanceModifier: InheritanceModifier? = null,
    private var parameterModifier: ParameterModifier? = null,
    private var platformModifier: PlatformModifier? = null
) : FirAbstractAnnotatedElement(session, psi) {
    fun addModifier(modifier: LighterASTNode) {
        val tokenType = modifier.tokenType
        when {
            CLASS_MODIFIER.contains(tokenType) -> this.classModifier = ClassModifier.valueOf(modifier.toString().toUpperCase())
            MEMBER_MODIFIER.contains(tokenType) -> this.memberModifiers += MemberModifier.valueOf(modifier.toString().toUpperCase())
            VISIBILITY_MODIFIER.contains(tokenType) -> this.visibilityModifier =
                VisibilityModifier.valueOf(modifier.toString().toUpperCase())
            FUNCTION_MODIFIER.contains(tokenType) -> this.functionModifiers += FunctionModifier.valueOf(modifier.toString().toUpperCase())
            PROPERTY_MODIFIER.contains(tokenType) -> this.propertyModifier = PropertyModifier.valueOf(modifier.toString().toUpperCase())
            INHERITANCE_MODIFIER.contains(tokenType) -> this.inheritanceModifier =
                InheritanceModifier.valueOf(modifier.toString().toUpperCase())
            PARAMETER_MODIFIER.contains(tokenType) -> this.parameterModifier = ParameterModifier.valueOf(modifier.toString().toUpperCase())
            PLATFORM_MODIFIER.contains(tokenType) -> this.platformModifier = PlatformModifier.valueOf(modifier.toString().toUpperCase())
        }
    }

    fun isEnum(): Boolean {
        return classModifier == ClassModifier.ENUM
    }

    fun isAnnotation(): Boolean {
        return classModifier == ClassModifier.ANNOTATION
    }

    fun isDataClass(): Boolean {
        return classModifier == ClassModifier.DATA
    }

    fun isInner(): Boolean {
        return classModifier == ClassModifier.INNER
    }

    fun isCompanion(): Boolean {
        return classModifier == ClassModifier.COMPANION
    }

    fun hasOverride(): Boolean {
        return memberModifiers.contains(MemberModifier.OVERRIDE)
    }

    fun hasLateinit(): Boolean {
        return memberModifiers.contains(MemberModifier.LATEINIT)
    }

    fun getVisibility(): Visibility {
        return visibilityModifier.toVisibility()
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

    fun getModality(): Modality? {
        return inheritanceModifier?.toModality()
    }

    fun hasVararg(): Boolean {
        return parameterModifier == ParameterModifier.VARARG
    }

    fun hasNoinline(): Boolean {
        return parameterModifier == ParameterModifier.NOINLINE
    }

    fun hasCrossinline(): Boolean {
        return parameterModifier == ParameterModifier.CROSSINLINE
    }

    fun hasExpect(): Boolean {
        return platformModifier == PlatformModifier.EXPECT
    }

    fun hasActual(): Boolean {
        return platformModifier == PlatformModifier.ACTUAL
    }
}
