/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinBlockStringTemplateEntryStub;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub;

import java.util.List;

/**
 * Represents a block interpolation in a string template using {@code ${expression}}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val s = "Sum: ${a + b}"
 * //            ^______^
 * }</pre>
 */
public class KtBlockStringTemplateEntry extends KtStringTemplateEntryWithExpression {
    public KtBlockStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtBlockStringTemplateEntry(@NotNull KotlinBlockStringTemplateEntryStub stub) {
        super(stub, KtStubBasedElementTypes.LONG_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBlockStringTemplateEntry(this, data);
    }

    @NotNull
    @Override
    public List<KtExpression> getExpressions() {
        // not green stub is used on purpose as it should be more performant
        // to search directly via ast
        KotlinPlaceHolderWithTextStub<? extends KtStringTemplateEntry> stub = getStub();

        // We may potentially search expressions via stubs only if it is the correct code (only one expression)
        // otherwise ast has to be loaded
        if (stub != null && !((KotlinBlockStringTemplateEntryStub)stub).getHasMultipleExpressions()) {
            return super.getExpressions();
        }

        return PsiTreeUtil.getChildrenOfTypeAsList(this, KtExpression.class);
    }
}
