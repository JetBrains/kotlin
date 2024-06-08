/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

/**
 * The code example:
 * <pre>{@code
 * class SimpleClass(i: Int) {
 *     constructor(s: String): this(s.toInt())
 * //                         ^____^
 * }
 * }</pre>
 */
public class KtConstructorDelegationReferenceExpression extends KtExpressionImpl implements KtReferenceExpression, KtResolvableCall {
    public KtConstructorDelegationReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isThis() {
        return findChildByType(KtTokens.THIS_KEYWORD) != null;
    }
}
