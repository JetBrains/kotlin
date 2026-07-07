/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersKt;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.KtNodeTypes.PROPERTY_DELEGATE;
import static org.jetbrains.kotlin.lexer.KtTokens.EQ;
import static org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt.isKtFile;

/**
 * Represents a property declaration with an optional getter and setter.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    val name: String = "Kotlin"
 * // ^_________________________^
 * // The entire property
 * }</pre>
 */
public class KtProperty extends KtTypeParameterListOwnerStub<KotlinPropertyStub>
        implements KtVariableDeclaration {

    private static final Logger LOG = Logger.getInstance(KtProperty.class);

    public KtProperty(@NotNull ASTNode node) {
        super(node);
    }

    public KtProperty(@NotNull KotlinPropertyStub stub) {
        super(stub, KtStubBasedElementTypes.PROPERTY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitProperty(this, data);
    }

    @Override
    public boolean isVar() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            return stub.isVar();
        }

        return getNode().findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    /**
     * Whether the property is local.
     * <p>
     * <b>Note</b>: a member property of a local class is not local
     */
    public boolean isLocal() {
        return !isTopLevel() && !isMember();
    }

    /**
     * Returns {@code true} if this property is a member of a class, object, or script (as opposed to a local variable
     * or a top-level property).
     */
    public boolean isMember() {
        return KtPsiUtilKt.getContainingClassOrScript(this) != null;
    }

    /**
     * Returns {@code true} if this property is declared directly at the top level of a file.
     */
    public boolean isTopLevel() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return isKtFile(getParent());
    }

    /** Always {@code null}: a property has no value parameter list. */
    @Nullable
    @Override
    public KtParameterList getValueParameterList() {
        return null;
    }

    /** Always empty: a property has no value parameters. */
    @NotNull
    @Override
    public List<KtParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtTypeReference getReceiverTypeReference() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            if (!stub.isExtension()) {
                return null;
            }
            else {
                return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
            }
        }
        return getReceiverTypeRefByTree();
    }

    @Nullable
    private KtTypeReference getReceiverTypeRefByTree() {
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            IElementType tt = node.getElementType();
            if (tt == KtTokens.COLON) break;

            if (tt == KtNodeTypes.TYPE_REFERENCE) {
                return (KtTypeReference) node.getPsi();
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @Nullable
    public KtTypeReference getTypeReference() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            if (!stub.getHasReturnTypeRef()) {
                return null;
            }
            else {
                List<KtTypeReference> typeReferences = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_REFERENCE);
                int returnTypeRefPositionInPsi = stub.isExtension() ? 1 : 0;
                if (typeReferences.size() <= returnTypeRefPositionInPsi) {
                    LOG.error("Invalid stub structure built for property:\n" + getText());
                    return null;
                }
                return typeReferences.get(returnTypeRefPositionInPsi);
            }
        }
        return TypeRefHelpersKt.getTypeReference(this);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.setPropertyTypeReference(this, typeRef)}
     * instead.
     */
    @Override
    @Nullable
    @Deprecated
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        return KtPsiMutationService.getInstance().setPropertyTypeReference(this, typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(KtTokens.COLON);
    }

    /**
     * Returns the {@code =} token that precedes the initializer, or {@code null} if this property has no initializer.
     */
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    /**
     * Returns the explicitly declared accessors (getter and/or setter), in source order; empty if none are declared.
     */
    @NotNull
    public List<KtPropertyAccessor> getAccessors() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.PROPERTY_ACCESSOR);
    }

    /**
     * Returns the explicitly declared getter, or {@code null} if the property uses the default getter.
     */
    @Nullable
    public KtPropertyAccessor getGetter() {
        for (KtPropertyAccessor accessor : getAccessors()) {
            if (accessor.isGetter()) return accessor;
        }

        return null;
    }

    /**
     * Returns the explicitly declared setter, or {@code null} if the property is read-only or uses the default setter.
     */
    @Nullable
    public KtPropertyAccessor getSetter() {
        for (KtPropertyAccessor accessor : getAccessors()) {
            if (accessor.isSetter()) return accessor;
        }

        return null;
    }

    /**
     * Returns the explicit backing field declaration ({@code field ...}), or {@code null} if this property has none.
     */
    @Nullable
    public KtBackingField getFieldDeclaration() {
        for (KtBackingField field : getStubOrPsiChildrenAsList(KtStubBasedElementTypes.BACKING_FIELD)) {
            return field;
        }

        return null;
    }

    /**
     * Returns {@code true} if this property is delegated (declared with {@code by}).
     */
    public boolean hasDelegate() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            return stub.getHasDelegate();
        }

        return getDelegate() != null;
    }

    /**
     * Returns the property delegate ({@code by ...}), or {@code null} if this property is not delegated.
     */
    @Nullable
    public KtPropertyDelegate getDelegate() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null && !stub.getHasDelegate()) {
            return null;
        }

        return (KtPropertyDelegate) findChildByType(PROPERTY_DELEGATE);
    }

    /**
     * Returns {@code true} if this property is delegated and the delegate expression is present.
     */
    public boolean hasDelegateExpression() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            return stub.getHasDelegateExpression();
        }

        return getDelegateExpression() != null;
    }

    /**
     * Returns the delegate expression (the part after {@code by}), or {@code null} if this property is not delegated.
     */
    @Nullable
    public KtExpression getDelegateExpression() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null && !stub.getHasDelegateExpression()) {
            return null;
        }

        KtPropertyDelegate delegate = getDelegate();
        if (delegate != null) {
            return delegate.getExpression();
        }

        return null;
    }

    @Override
    public boolean hasInitializer() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            return stub.getHasInitializer();
        }

        return getInitializer() != null;
    }

    @Override
    @Nullable
    public KtExpression getInitializer() {
        KotlinPropertyStub stub = getGreenStub();
        if (stub != null) {
            if (!stub.getHasInitializer()) {
                return null;
            }
        }

        return PsiTreeUtil.getNextSiblingOfType(findChildByType(EQ), KtExpression.class);
    }

    /**
     * Returns {@code true} if this property has either a delegate expression or an initializer.
     */
    public boolean hasDelegateExpressionOrInitializer() {
        return hasDelegateExpression() || hasInitializer();
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.setPropertyInitializer(this, initializer)}
     * instead.
     */
    @Nullable
    @Deprecated
    public KtExpression setInitializer(@Nullable KtExpression initializer) {
        return KtPsiMutationService.getInstance().setPropertyInitializer(this, initializer);
    }

    /**
     * Returns the delegate expression if this property is delegated, otherwise the initializer expression, or
     * {@code null} if the property has neither.
     */
    @Nullable
    public KtExpression getDelegateExpressionOrInitializer() {
        KtExpression expression = getDelegateExpression();
        if (expression == null) {
            return getInitializer();
        }
        return expression;
    }

    @Override
    @NotNull
    public PsiElement getValOrVarKeyword() {
        PsiElement element = findChildByType(KtTokens.VAL_VAR);
        assert element != null : "Val or var should always exist for property" + this.getText();
        return element;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    /**
     * Always returns {@code false}: changes inside a property never affect the out-of-code-block modification count.
     *
     * <p>Kept for compatibility with potential plugins.
     */
    @SuppressWarnings({"unused", "MethodMayBeStatic"})
    public boolean shouldChangeModificationCount(PsiElement place) {
        // Suppress Java check for out-of-block
        return false;
    }

    /**
     * Returns {@code true} if this property has a body: an initializer, a delegate, or an accessor with a body.
     */
    public boolean hasBody() {
        if (hasDelegateExpressionOrInitializer()) return true;
        KtPropertyAccessor getter = getGetter();
        if (getter != null && getter.hasBody()) {
            return true;
        }
        KtPropertyAccessor setter = getSetter();
        if (setter != null && setter.hasBody()) {
            return true;
        }
        return false;
    }
}
