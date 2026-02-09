/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub;

/**
 * Represents an escape sequence in a string template.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val s = "Hello\nWorld"
 * //            ^^
 * }</pre>
 */
public class KtEscapeStringTemplateEntry extends KtStringTemplateEntry {
    public KtEscapeStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtEscapeStringTemplateEntry(@NotNull KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry> stub) {
        super(stub, KtStubBasedElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitEscapeStringTemplateEntry(this, data);
    }

    public String getUnescapedValue() {
        return StringUtil.unescapeStringCharacters(getText());
    }
}
