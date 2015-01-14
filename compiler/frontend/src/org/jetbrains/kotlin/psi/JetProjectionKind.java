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

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.JetSingleValueToken;
import org.jetbrains.kotlin.lexer.JetTokens;

public enum JetProjectionKind {
    IN(JetTokens.IN_KEYWORD), OUT(JetTokens.OUT_KEYWORD), STAR(JetTokens.MUL), NONE(null);

    private final JetSingleValueToken token;

    JetProjectionKind(@Nullable JetSingleValueToken token) {
        this.token = token;
    }

    @Nullable
    public JetSingleValueToken getToken() {
        return token;
    }
}
