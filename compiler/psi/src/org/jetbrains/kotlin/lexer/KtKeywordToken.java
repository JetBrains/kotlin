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

public class KtKeywordToken extends KtSingleValueToken {

    /**
     * Generate keyword (identifier that has a keyword meaning in all possible contexts)
     */
    public static KtKeywordToken keyword(String value) {
        return keyword(value, value);
    }

    public static KtKeywordToken keyword(String debugName, String value) {
        return new KtKeywordToken(debugName, value, false);
    }

    /**
     * Generate soft keyword (identifier that has a keyword meaning only in some contexts)
     */
    public static KtKeywordToken softKeyword(String value) {
        return new KtKeywordToken(value, value, true);
    }

    private final boolean myIsSoft;

    protected KtKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft) {
        super(debugName, value);
        myIsSoft = isSoft;
    }

    public boolean isSoft() {
        return myIsSoft;
    }
}
