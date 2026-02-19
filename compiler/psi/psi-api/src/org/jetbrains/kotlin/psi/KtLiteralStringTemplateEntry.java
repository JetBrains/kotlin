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
 * Represents a literal text segment in a string template without any interpolation.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val simple = "Hello, World"
 * //            ^__________^
 * // The entire content of the template is a simple string literal
 *
 * val complex = "Hello, ${name}!"
 * //             ^_____^       ^
 * // Non-interpolated parts are simple string literals
 * }</pre>
 */
public class KtLiteralStringTemplateEntry extends KtStringTemplateEntry {
    public KtLiteralStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtLiteralStringTemplateEntry(@NotNull KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry> stub) {
        super(stub, KtStubBasedElementTypes.LITERAL_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitLiteralStringTemplateEntry(this, data);
    }
}
