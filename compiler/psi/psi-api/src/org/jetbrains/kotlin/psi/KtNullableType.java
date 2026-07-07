/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;
import org.jetbrains.kotlin.resolution.KtResolvable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a nullable type marked with a question mark.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val name: String? = null
 * //        ^_____^
 * }</pre>
 */
public class KtNullableType extends KtElementImplStub<KotlinPlaceHolderStub<KtNullableType>> implements KtTypeElement, KtResolvable {
    public KtNullableType(@NotNull ASTNode node) {
        super(node);
    }

    public KtNullableType(@NotNull KotlinPlaceHolderStub<KtNullableType> stub) {
        super(stub, KtStubBasedElementTypes.NULLABLE_TYPE);
    }

    /**
     * Returns the AST node of the {@code ?} token that marks the type as nullable.
     */
    @NotNull
    public ASTNode getQuestionMarkNode() {
        return getNode().findChildByType(KtTokens.QUEST);
    }

    @NotNull
    @Override
    public List<KtTypeReference> getTypeArgumentsAsTypes() {
        KtTypeElement innerType = getInnerType();
        return innerType == null ? Collections.emptyList() : innerType.getTypeArgumentsAsTypes();
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitNullableType(this, data);
    }

    /**
     * Returns the underlying non-nullable type (the part before {@code ?}), or {@code null} if it is absent in
     * incomplete code.
     */
    @Nullable
    @IfNotParsed
    public KtTypeElement getInnerType() {
        return KtStubbedPsiUtil.getStubOrPsiChild(this, KtTokenSets.TYPE_ELEMENT_TYPES, KtTypeElement.ARRAY_FACTORY);
    }

    /**
     * Returns the modifier list attached to this nullable type, or {@code null} if there is none.
     */
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtModifierList getModifierList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.MODIFIER_LIST);
    }

    /**
     * Returns the annotation entries attached to this nullable type, or an empty list if there are none.
     */
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        KtModifierList modifierList = getModifierList();
        return modifierList != null ? modifierList.getAnnotationEntries() : Collections.emptyList();
    }
}
