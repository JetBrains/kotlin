/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement;
import org.jetbrains.kotlin.psi.KtImplementationDetail;

/**
 * Base implementation of {@link KDocElement}, wrapping the corresponding AST node.
 *
 * <p>This is an internal implementation base class of the KDoc PSI, shared by the tag element types {@code KDocTag} and
 * {@code KDocSection}. It is not intended to be used or subclassed outside of the PSI implementation.
 */
public abstract class KDocElementImpl extends ASTWrapperPsiElement implements KDocElement {
    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @KtImplementationDetail
    public KDocElementImpl(@NotNull ASTNode node) {
        super(node);
    }
}
