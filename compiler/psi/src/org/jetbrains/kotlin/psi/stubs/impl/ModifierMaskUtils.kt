/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import org.jetbrains.kotlin.psi.KtModifierList

import org.jetbrains.kotlin.lexer.KtTokens.MODIFIER_KEYWORDS_ARRAY
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

object ModifierMaskUtils {
    init {
        assert(MODIFIER_KEYWORDS_ARRAY.size <= 64) { "Current implementation depends on the ability to represent modifier list as bit mask" }
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

    @JvmStatic
    fun maskHasModifier(mask: Long, modifierToken: KtModifierKeywordToken): Boolean {
        val index = MODIFIER_KEYWORDS_ARRAY.indexOf(modifierToken)
        assert(index >= 0) { "All JetModifierKeywordTokens should be present in MODIFIER_KEYWORDS_ARRAY" }
        return (mask and (1L shl index)) != 0L
    }

    @JvmStatic
    fun maskToString(mask: Long): String {
        val sb = StringBuilder()
        sb.append("[")
        var first = true
        for (modifierKeyword in MODIFIER_KEYWORDS_ARRAY) {
            if (maskHasModifier(mask, modifierKeyword)) {
                if (!first) {
                    sb.append(" ")
                }
                sb.append(modifierKeyword.value)
                first = false
            }
        }
        sb.append("]")
        return sb.toString()
    }

}
