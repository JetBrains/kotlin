/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.MODIFIER_KEYWORDS_ARRAY
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub

object ModifierMaskUtils {
    init {
        @OptIn(KtImplementationDetail::class)
        assert(MODIFIER_KEYWORDS_ARRAY.size + KotlinModifierListStub.SpecialFlag.entries.size <= 64) {
            "Current implementation depends on the ability to represent modifier list as bit mask"
        }
    }

    @JvmStatic
    fun computeMaskFromModifierList(modifierList: KtModifierList): Long = computeMask { modifierList.hasModifier(it) }

    @JvmStatic
    fun computeMask(hasModifier: (KtModifierKeywordToken) -> Boolean): Long {
        var mask = 0L
        for ((index, modifierKeywordToken) in MODIFIER_KEYWORDS_ARRAY.withIndex()) {
            if (hasModifier(modifierKeywordToken)) {
                mask = mask or (1L shl index)
            }
        }

        return mask
    }

    @KtImplementationDetail
    fun computeMaskForSpecialFlags(hasModifier: (KotlinModifierListStub.SpecialFlag) -> Boolean): Long {
        val specialFlagOffset = MODIFIER_KEYWORDS_ARRAY.size
        var mask = 0L
        for ((index, specialFlag) in KotlinModifierListStub.SpecialFlag.entries.withIndex()) {
            if (hasModifier(specialFlag)) {
                mask = mask or (1L shl (specialFlagOffset + index))
            }
        }

        return mask
    }

    @JvmStatic
    fun maskHasModifier(mask: Long, modifierToken: KtModifierKeywordToken): Boolean {
        val index = MODIFIER_KEYWORDS_ARRAY.indexOf(modifierToken)
        assert(index >= 0) { "All KtModifierKeywordTokens should be present in MODIFIER_KEYWORDS_ARRAY" }
        return maskHas(mask, index)
    }

    @JvmStatic
    @KtImplementationDetail
    fun maskHasSpecialFlag(mask: Long, flag: KotlinModifierListStub.SpecialFlag): Boolean {
        val specialFlagOffset = MODIFIER_KEYWORDS_ARRAY.size
        return maskHas(mask, specialFlagOffset + flag.ordinal)
    }

    private fun maskHas(mask: Long, flagIndex: Int): Boolean = (mask and (1L shl flagIndex)) != 0L

    @JvmStatic
    fun maskToString(mask: Long): String {
        val sb = StringBuilder()
        sb.append("[")
        var first = true
        fun renderFlag(value: String) {
            if (first) {
                first = false
            } else {
                sb.append(" ")
            }

            sb.append(value)
        }

        for (modifierKeyword in MODIFIER_KEYWORDS_ARRAY) {
            if (maskHasModifier(mask, modifierKeyword)) {
                renderFlag(modifierKeyword.value)
            }
        }

        @OptIn(KtImplementationDetail::class)
        for (specialFlag in KotlinModifierListStub.SpecialFlag.entries) {
            if (maskHasSpecialFlag(mask, specialFlag)) {
                renderFlag(specialFlag.name)
            }
        }

        sb.append("]")
        return sb.toString()
    }
}
