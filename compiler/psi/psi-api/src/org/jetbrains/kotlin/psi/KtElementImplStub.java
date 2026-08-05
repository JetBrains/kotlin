/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

import java.util.Arrays;
import java.util.List;

public class KtElementImplStub<T extends StubElement<?>> extends StubBasedPsiElementBase<T>
        implements KtElement, StubBasedPsiElement<T> {
    public KtElementImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtElementImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    @SuppressWarnings("deprecation") // KT-78356
    public String toString() {
        return getElementType().toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof KtVisitor) {
            accept((KtVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @NotNull
    @Override
    public KtFile getContainingKtFile() {
        PsiFile file = getContainingFile();
        if (!(file instanceof KtFile)) {
            // KtElementImpl.copy() might be the reason for this exception
            String fileString = "";
            if (file.isValid()) {
                try {
                    fileString = " " + file.getText();
                }
                catch (Exception e) {
                    // ignore when failed to get file text
                }
            }
            // getNode() will fail if getContainingFile() returns not PsiFileImpl instance
            String nodeString = (file instanceof PsiFileImpl ? (" node = " + getNode()) : "");

            throw new IllegalStateException("KtElement not inside KtFile: " +
                                            file + fileString + " of type " + file.getClass() +
                                            " for element " + this + " of type " + this.getClass() + nodeString);
        }
        return (KtFile) file;
    }

    @Override
    public <D> void acceptChildren(@NotNull KtVisitor<Void, D> visitor, D data) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof KtElement) {
                ((KtElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitKtElement(this, data);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        KtPsiMutationService.getInstance().deleteElement(this);
    }

    @Override
    @KtNonPublicApi
    public void rawDelete() throws IncorrectOperationException {
        super.delete();
    }

    @Override
    @SuppressWarnings("deprecation") // KT-78356
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        return (references.length > 0) ? references[0] : null;
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return KotlinReferenceProvidersService.getReferencesFromProviders(this);
    }

    @NotNull
    @SuppressWarnings("deprecation") // KT-78356
    protected <PsiT extends KtElementImplStub<?>, StubT extends StubElement<?>> List<PsiT> getStubOrPsiChildrenAsList(
            @NotNull KtStubElementType<StubT, PsiT> elementType
    ) {
        return Arrays.asList(getStubOrPsiChildren(elementType, elementType.getArrayFactory()));
    }

    @NotNull
    @Override
    public KtElement getPsiOrParent() {
        return this;
    }

    /**
     * The expressions this element holds, taken from its stub, or an empty array if the stub does not have them.
     *
     * <p>An expression is stubbed only where {@link KtTokenSets#STUBBED_EXPRESSIONS} covers its element type, so a
     * caller has to fall back to the AST when nothing is found: the expression may still be there, merely not stubbed.
     *
     * <p>The expression is told apart from the other children by its element type alone. No child of a declaration
     * that is not its initializer, expression body or default value has one of these types, and the references
     * inside a type reference are grandchildren, out of reach of a direct lookup.
     *
     * <p>Nothing is looked up once the AST is loaded: the stub carries no data the AST does not have here, so the
     * caller may as well read the AST it already paid for.
     */
    @NotNull
    protected final KtExpression[] getExpressionsFromStub() {
        T stub = getStub();
        if (stub == null) return KtExpression.EMPTY_ARRAY;

        return stub.getChildrenByType(KtTokenSets.STUBBED_EXPRESSIONS, KtExpression.EMPTY_ARRAY);
    }

    /**
     * @see #getExpressionsFromStub()
     */
    @Nullable
    protected final KtExpression getExpressionFromStub() {
        KtExpression[] expressions = getExpressionsFromStub();
        return expressions.length != 0 ? expressions[0] : null;
    }
}
