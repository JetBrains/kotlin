/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub;
import org.jetbrains.kotlin.psi.utils.ConstantExpressionUtils;

/**
 * Represents a constant literal expression such as numbers, booleans, or characters.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val x = 42
 * //      ^^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtConstantExpression extends KtExpressionImplStub<KotlinConstantExpressionStub> {
    @KtImplementationDetail
    public KtConstantExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtConstantExpression(@NotNull KotlinConstantExpressionStub stub) {
        super(stub, ConstantExpressionUtils.toConstantExpressionElementType(stub.getKind()));
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitConstantExpression(this, data);
    }

    @Override
    public @NotNull String getText() {
        KotlinConstantExpressionStub stub = getGreenStub();
        if (stub != null) {
            return stub.getValue();
        }

        return super.getText();
    }
}
