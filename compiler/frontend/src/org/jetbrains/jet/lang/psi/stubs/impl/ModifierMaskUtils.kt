/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.impl

import org.jetbrains.jet.lang.psi.JetModifierList

import org.jetbrains.jet.lexer.JetTokens.MODIFIER_KEYWORDS_ARRAY
import org.jetbrains.jet.lexer.JetModifierKeywordToken
import kotlin.platform.platformStatic

public object ModifierMaskUtils {
    {
        assert(MODIFIER_KEYWORDS_ARRAY.size <= 32, "Current implementation depends on the ability to represent modifier list as bit mask")
    }

    platformStatic public fun computeMaskFromModifierList(modifierList: JetModifierList): Int = computeMask { modifierList.hasModifier(it) }

    platformStatic public fun computeMask(hasModifier: (JetModifierKeywordToken) -> Boolean): Int {
        var mask = 0
        for ((index, modifierKeywordToken) in MODIFIER_KEYWORDS_ARRAY.withIndices()) {
            if (hasModifier(modifierKeywordToken)) {
                mask = mask or (1 shl index)
            }
        }
        return mask
    }

    platformStatic public fun maskHasModifier(mask: Int, modifierToken: JetModifierKeywordToken): Boolean {
        val index = MODIFIER_KEYWORDS_ARRAY.indexOf(modifierToken)
        assert(index >= 0, "All JetModifierKeywordTokens should be present in MODIFIER_KEYWORDS_ARRAY")
        return (mask and (1 shl index)) != 0
    }

    platformStatic public fun maskToString(mask: Int): String {
        val sb = StringBuilder()
        sb.append("[")
        var first = true
        for (modifierKeyword in MODIFIER_KEYWORDS_ARRAY) {
            if (maskHasModifier(mask, modifierKeyword)) {
                if (!first) {
                    sb.append(" ")
                }
                sb.append(modifierKeyword.getValue())
                first = false
            }
        }
        sb.append("]")
        return sb.toString()
    }

}