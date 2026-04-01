/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.resolution.KtResolvable;

import java.util.Objects;

/**
 * A common base class for {@code this} and {@code super} expressions, both of which
 * refer to an instance receiver and may have an optional label qualifier.
 *
 * <p>The label can refer to a class, an extension function receiver, or any other
 * labeled scope — not only classes.</p>
 *
 * <h3>Examples:</h3>
 * <pre>{@code
 * class Foo : Bar() {
 *     fun baz() {
 *         this        // KtThisExpression — current class receiver
 *         this@Foo    // KtThisExpression with label — explicit class receiver
 *         super.baz() // KtSuperExpression — superclass receiver
 *     }
 * }
 *
 * fun String.ext() {
 *     this        // KtThisExpression — extension receiver
 *     this@ext    // KtThisExpression with label — explicit extension receiver
 * }
 * }</pre>
 *
 * @see KtThisExpression
 * @see KtSuperExpression
 */
public abstract class KtInstanceExpressionWithLabel extends KtExpressionWithLabel implements KtResolvable {

    public KtInstanceExpressionWithLabel(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Returns the reference expression corresponding to the {@code this} or {@code super} keyword itself.
     */
    @NotNull
    public KtReferenceExpression getInstanceReference() {
        return Objects.requireNonNull(findChildByType(KtNodeTypes.REFERENCE_EXPRESSION));
    }
}
