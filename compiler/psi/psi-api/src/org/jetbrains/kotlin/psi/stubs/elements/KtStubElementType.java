/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElementImplStub;

import java.util.function.Function;

public abstract class KtStubElementType<StubT extends StubElement<?>, PsiT extends KtElementImplStub<?>> extends IStubElementType<StubT, PsiT> {

    @NotNull
    private final Function<ASTNode, PsiT> psiFromAstFactory;
    @NotNull
    private final Function<StubT, PsiT> psiFromStubFactory;
    @NotNull
    private final PsiT[] emptyArray;
    @NotNull
    private final ArrayFactory<PsiT> arrayFactory;
    private final boolean isExpression;

    @SuppressWarnings("unchecked")
    public KtStubElementType(
            @NotNull @NonNls String debugName,
            @NotNull Function<ASTNode, PsiT> psiFromAstFactory,
            @NotNull Function<StubT, PsiT> psiFromStubFactory,
            @NotNull ArrayFactory<PsiT> arrayFactory,
            boolean isExpression
    ) {
        super(debugName, KotlinLanguage.INSTANCE);
        this.psiFromAstFactory = psiFromAstFactory;
        this.psiFromStubFactory = psiFromStubFactory;
        this.isExpression = isExpression;
        emptyArray = arrayFactory.create(0);
        this.arrayFactory = count -> {
            if (count == 0) {
                return emptyArray;
            }
            return arrayFactory.create(count);
        };
    }

    @NotNull
    public PsiT createPsiFromAst(@NotNull ASTNode node) {
        return psiFromAstFactory.apply(node);
    }

    @Override
    @NotNull
    public PsiT createPsi(@NotNull StubT stub) {
        return psiFromStubFactory.apply(stub);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "kotlin." + getDebugName();
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        return createStubDependingOnParent(node);
    }

    private static boolean createStubDependingOnParent(ASTNode node) {
        ASTNode parent = node.getTreeParent();
        IElementType parentType = parent.getElementType();
        if (parentType instanceof IStubElementType) {
            return ((IStubElementType) parentType).shouldCreateStub(parent);
        }
        if (parentType instanceof IStubFileElementType) {
            return true;
        }
        return false;
    }

    @Override
    public void indexStub(@NotNull StubT stub, @NotNull IndexSink sink) {
        // do not force inheritors to implement this method
    }

    @NotNull
    public ArrayFactory<PsiT> getArrayFactory() {
        return arrayFactory;
    }

    /**
     * @return true when the {@link PsiT} implements {@link KtExpression}.
     */
    public boolean isExpression() {
        return isExpression;
    }
}
