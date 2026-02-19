/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub;

/**
 * Represents a simple variable interpolation in a string template using {@code $name}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val s = "Hello, $name"
 * //              ^___^
 * }</pre>
 */
public class KtSimpleNameStringTemplateEntry extends KtStringTemplateEntryWithExpression {
    public KtSimpleNameStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtSimpleNameStringTemplateEntry(@NotNull KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry> stub) {
        super(stub, KtStubBasedElementTypes.SHORT_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSimpleNameStringTemplateEntry(this, data);
    }
}
