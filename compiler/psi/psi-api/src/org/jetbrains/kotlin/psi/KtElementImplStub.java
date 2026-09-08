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
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;


/**
 * Base implementation of {@link KtElement} that may be backed either by the AST tree or by a stub.
 *
 * <p>Stubs are a compact, serializable representation of a declaration's structure that can be built and queried without parsing the full
 * source. They power fast indexing and are used for library and decompiled sources, so that such files do not have to be fully parsed until
 * their bodies are actually needed. Subclasses read their data from the stub when one is present, and fall back to the AST otherwise.
 *
 * <p>This is an internal implementation base class of the Kotlin PSI and is not intended to be used or subclassed outside of the
 * PSI implementation.
 *
 * @param <T> the type of stub backing this element
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtElementImplStub<T extends StubElement<?>> extends StubBasedPsiElementBase<T>
        implements KtElement, StubBasedPsiElement<T> {
    @KtImplementationDetail
    public KtElementImplStub(@NotNull T stub, @NotNull IElementType nodeType) {
        super(stub, nodeType);
    }

    @KtImplementationDetail
    public KtElementImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public IElementType getIElementType() {
        return getElementTypeImpl();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getIElementType().toString();
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
    @SuppressWarnings("deprecation") // the overridden KtElement#getReference is deprecated
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
