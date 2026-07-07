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
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtTokens;

/**
 * The kind of variance projection applied to a type argument in a {@link KtTypeProjection}.
 *
 * <ul>
 *     <li>{@link #IN} — a contravariant projection, written {@code in T}.</li>
 *     <li>{@link #OUT} — a covariant projection, written {@code out T}.</li>
 *     <li>{@link #STAR} — a star projection, written {@code *}.</li>
 *     <li>{@link #NONE} — an invariant type argument with no projection keyword.</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun copy(from: Array<out Any>, to: Array<in Any>) {}
 * //                   ^^^                 ^^
 * //                   OUT                 IN
 * }</pre>
 */
public enum KtProjectionKind {
    /** A contravariant projection, written {@code in T}. */
    IN(KtTokens.IN_KEYWORD),
    /** A covariant projection, written {@code out T}. */
    OUT(KtTokens.OUT_KEYWORD),
    /** A star projection, written {@code *}. */
    STAR(KtTokens.MUL),
    /** An invariant type argument with no projection keyword. */
    NONE(null);

    private final KtSingleValueToken token;

    KtProjectionKind(@Nullable KtSingleValueToken token) {
        this.token = token;
    }

    /**
     * Returns the keyword token that denotes this projection ({@code in}, {@code out}, or {@code *}), or {@code null}
     * for {@link #NONE}.
     */
    @Nullable
    public KtSingleValueToken getToken() {
        return token;
    }
}
