/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.Variance

open class ModifierList(var modifiers: Long = ModifierFlag.NONE.value) {
    val annotations: MutableList<LighterASTNode> = mutableListOf()
    var contextLists: MutableList<LighterASTNode> = mutableListOf()

    fun addModifier(modifier: LighterASTNode, isInClass: Boolean = false) {
        when (val tokenType = modifier.tokenType) {
            KtTokens.CONST_KEYWORD -> {
                // Specific case because CONST may exist both on parameter and property
                setFlag(ModifierFlag.PROPERTY_CONST)
                setFlag(ModifierFlag.PARAMETER_CONST)
            }
            KtTokens.INLINE_KEYWORD -> {
                setFlag(if (isInClass) ModifierFlag.CLASS_INLINE else ModifierFlag.FUNCTION_INLINE)
            }
            KtTokens.VALUE_KEYWORD -> {
                setFlag(ModifierFlag.CLASS_VALUE)
            }
            else -> {
                setFlag(ModifierFlag.ElementTypeToModifierFlagMap[tokenType])
            }
        }
    }

    fun isEnum(): Boolean = hasFlag(ModifierFlag.CLASS_ENUM)

    fun isAnnotation(): Boolean = hasFlag(ModifierFlag.CLASS_ANNOTATION)

    fun isDataClass(): Boolean = hasFlag(ModifierFlag.CLASS_DATA)

    fun isInlineClass(): Boolean = hasFlag(ModifierFlag.CLASS_INLINE)

    fun isValueClass(): Boolean = hasFlag(ModifierFlag.CLASS_VALUE)

    fun isInner(): Boolean = hasFlag(ModifierFlag.CLASS_INNER)

    fun isCompanion(): Boolean = hasFlag(ModifierFlag.CLASS_COMPANION)

    fun isFunctionalInterface(): Boolean = hasFlag(ModifierFlag.CLASS_FUN)

    fun hasOverride(): Boolean = hasFlag(ModifierFlag.MEMBER_OVERRIDE)

    fun hasLateinit(): Boolean = hasFlag(ModifierFlag.MEMBER_LATEINIT)

    fun getVisibility(publicByDefault: Boolean = false): Visibility {
        return when {
            hasFlag(ModifierFlag.VISIBILITY_PRIVATE) -> Visibilities.Private
            hasFlag(ModifierFlag.VISIBILITY_PUBLIC) -> Visibilities.Public
            hasFlag(ModifierFlag.VISIBILITY_PROTECTED) -> Visibilities.Protected
            hasFlag(ModifierFlag.VISIBILITY_INTERNAL) -> Visibilities.Internal
            else -> if (publicByDefault) Visibilities.Public else Visibilities.Unknown
        }
    }

    fun hasTailrec(): Boolean = hasFlag(ModifierFlag.FUNCTION_TAILREC)

    fun hasOperator(): Boolean = hasFlag(ModifierFlag.FUNCTION_OPERATOR)

    fun hasInfix(): Boolean = hasFlag(ModifierFlag.FUNCTION_INFIX)

    fun hasInline(): Boolean = hasFlag(ModifierFlag.FUNCTION_INLINE)

    fun hasExternal(): Boolean = hasFlag(ModifierFlag.FUNCTION_EXTERNAL)

    fun hasSuspend(): Boolean = hasFlag(ModifierFlag.FUNCTION_SUSPEND)

    fun hasStatic(): Boolean = hasFlag(ModifierFlag.STATIC)

    fun isConst(): Boolean = hasFlag(ModifierFlag.PROPERTY_CONST)

    fun hasModality(modality: Modality): Boolean {
        return when {
            modality == Modality.FINAL && hasFlag(ModifierFlag.INHERITANCE_FINAL) -> true
            modality == Modality.SEALED && hasFlag(ModifierFlag.INHERITANCE_SEALED) -> true
            modality == Modality.ABSTRACT && hasFlag(ModifierFlag.INHERITANCE_ABSTRACT) -> true
            modality == Modality.OPEN && hasFlag(ModifierFlag.INHERITANCE_OPEN) -> true
            else -> false
        }
    }

    fun getModality(isClassOrObject: Boolean): Modality? {
        return when {
            hasFlag(ModifierFlag.INHERITANCE_FINAL) -> Modality.FINAL
            hasFlag(ModifierFlag.INHERITANCE_SEALED) -> if (isClassOrObject) Modality.SEALED else null
            hasFlag(ModifierFlag.INHERITANCE_ABSTRACT) -> Modality.ABSTRACT
            hasFlag(ModifierFlag.INHERITANCE_OPEN) -> Modality.OPEN
            else -> null
        }
    }

    fun getVariance(): Variance {
        return when {
            hasFlag(ModifierFlag.VARIANCE_IN) -> Variance.IN_VARIANCE
            hasFlag(ModifierFlag.VARIANCE_OUT) -> Variance.OUT_VARIANCE
            else -> Variance.INVARIANT
        }
    }

    fun hasVararg(): Boolean = hasFlag(ModifierFlag.PARAMETER_VARARG)

    fun hasNoinline(): Boolean = hasFlag(ModifierFlag.PARAMETER_NOINLINE)

    fun hasCrossinline(): Boolean = hasFlag(ModifierFlag.PARAMETER_CROSSINLINE)

    fun hasExpect(): Boolean = hasFlag(ModifierFlag.PLATFORM_EXPECT)

    fun hasActual(): Boolean = hasFlag(ModifierFlag.PLATFORM_ACTUAL)

    fun hasConst(): Boolean = hasFlag(ModifierFlag.PARAMETER_CONST)

    protected fun hasFlag(flag: ModifierFlag): Boolean = (modifiers and flag.value) == flag.value

    protected fun setFlag(flag: ModifierFlag?) {
        if (flag != null) {
            modifiers = modifiers or flag.value
        }
    }

    override fun toString(): String {
        val result = StringBuilder()
        var firstAppend = true
        for (value in ModifierFlag.entries) {
            if (hasFlag(value) && value != ModifierFlag.NONE) {
                if (firstAppend) {
                    firstAppend = false
                } else {
                    result.append(" ")
                }
                result.append(value.name)
            }
        }
        return result.toString()
    }
}
