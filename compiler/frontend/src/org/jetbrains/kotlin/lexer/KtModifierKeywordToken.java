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

package org.jetbrains.kotlin.lexer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/* Modifier keyword is a keyword that can be used in annotation position as part of modifier list.*/
public final class KtModifierKeywordToken extends KtKeywordToken {

    /**
     * Generate keyword (identifier that has a keyword meaning in all possible contexts)
     */
    public static KtModifierKeywordToken keywordModifier(String value) {
        return new KtModifierKeywordToken(value, value, false);
    }

    /**
     * Generate soft keyword (identifier that has a keyword meaning only in some contexts)
     */
    public static KtModifierKeywordToken softKeywordModifier(String value) {
        return new KtModifierKeywordToken(value, value, true);
    }

    private KtModifierKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft) {
        super(debugName, value, isSoft);
    }
}
