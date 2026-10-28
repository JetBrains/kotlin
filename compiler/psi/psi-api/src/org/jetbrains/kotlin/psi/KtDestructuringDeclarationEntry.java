/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.impl.ResolveScopeManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import kotlin.DeprecationLevel;
import kotlin.ReplaceWith;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.stubs.KotlinDestructuringDeclarationEntryStub;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.lexer.KtTokens.EQ;

/**
 * Represents a single entry in a destructuring declaration.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val (x, y) = pair
 * //   ^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtDestructuringDeclarationEntry extends KtNamedDeclarationStub<KotlinDestructuringDeclarationEntryStub>
        implements KtVariableDeclaration, KtResolvableCall {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    public static final KtDestructuringDeclarationEntry[] EMPTY_ARRAY = new KtDestructuringDeclarationEntry[0];

    @KtImplementationDetail
    public KtDestructuringDeclarationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtDestructuringDeclarationEntry(@NotNull KotlinDestructuringDeclarationEntryStub stub) {
        super(stub, KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY);
    }

    /**
     * Keeps the non-stubbed renaming behavior: the new name is not quoted, and a missing name identifier is an error.
     */
    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        KtPsiMutationService mutationService = KtPsiMutationService.getInstanceOrNull();
        if (mutationService != null) return mutationService.setNamedDeclarationName(this, name);

        PsiElement identifier = getNameIdentifier();
        if (identifier == null) throw new IncorrectOperationException();

        return identifier.replace(new KtPsiFactory(getProject()).createNameIdentifier(name));
    }

    @Override
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtNodeTypes.TYPE_REFERENCE, KtTypeReference.class);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.setDestructuringDeclarationEntryTypeReference(this, typeRef)}
     * instead.
     */
    @Override
    @Nullable
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.setDestructuringDeclarationEntryTypeReference(this, typeRef)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.setDestructuringDeclarationEntryTypeReference(typeRef)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.setDestructuringDeclarationEntryTypeReference"
            ),
            level = DeprecationLevel.HIDDEN
    )
    @Deprecated
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        return KtPsiMutationService.getInstance().setDestructuringDeclarationEntryTypeReference(this, typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(KtTokens.COLON);
    }

    /** Always {@code null}: a destructuring entry has no value parameter list. */
    @Nullable
    @Override
    public KtParameterList getValueParameterList() {
        return null;
    }

    /** Always empty: a destructuring entry has no value parameters. */
    @NotNull
    @Override
    public List<KtParameter> getValueParameters() {
        return Collections.emptyList();
    }

    /** Always {@code null}: a destructuring entry cannot have an extension receiver. */
    @Nullable
    @Override
    public KtTypeReference getReceiverTypeReference() {
        return null;
    }

    /** Always {@code null}: a destructuring entry cannot declare type parameters. */
    @Nullable
    @Override
    public KtTypeParameterList getTypeParameterList() {
        return null;
    }

    /** Always {@code null}: a destructuring entry cannot have a {@code where} clause. */
    @Nullable
    @Override
    public KtTypeConstraintList getTypeConstraintList() {
        return null;
    }

    /** Always empty: a destructuring entry has no type constraints. */
    @NotNull
    @Override
    public List<KtTypeConstraint> getTypeConstraints() {
        return Collections.emptyList();
    }

    /** Always empty: a destructuring entry cannot declare type parameters. */
    @NotNull
    @Override
    public List<KtTypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDestructuringDeclarationEntry(this, data);
    }

    @Override
    public boolean isVar() {
        KotlinDestructuringDeclarationEntryStub stub = getGreenStub();
        if (stub != null) {
            return stub.isVar();
        }

        return findChildByType(KtTokens.VAR_KEYWORD) != null || getDestructuringDeclaration().getNode().findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    /**
     * Returns the name reference after {@code =} which renames the entry in a name-based destructuring declaration
     * (e.g., {@code first} in {@code (val a = first) = pair}), or {@code null} if the entry is not renamed.
     */
    @Nullable
    @Override
    public KtNameReferenceExpression getInitializer() {
        if (getGreenStub() != null) {
            // The only name reference child of an entry is its initializer
            return getStubOrPsiChild(KtNodeTypes.REFERENCE_EXPRESSION, KtNameReferenceExpression.class);
        }

        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), KtNameReferenceExpression.class);
    }

    /**
     * Returns the equals sign before the initializer if it is present.
     */
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(EQ);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    /**
     * Returns the destructuring declaration that contains this entry.
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * val (x, y) = pair
     * //^_____________^
     * }</pre>
     */
    @NotNull
    public KtDestructuringDeclaration getDestructuringDeclaration() {
        KotlinDestructuringDeclarationEntryStub stub = getGreenStub();
        PsiElement parent = stub != null ? stub.getParentStub().getPsi() : getParent();
        if (parent instanceof PsiErrorElement) {
            // For top-level destructuring declarations we report an error in the parser, so the entries are wrapped in an error element.
            // TODO(KT-58563): After the reporting is moved out of the parser, this workaround can be removed.
            parent = parent.getParent();
        }

        if (!(parent instanceof KtDestructuringDeclaration)) {
            String parentClass = parent != null ? parent.getClass().getName() : null;
            throw new IllegalStateException("Unexpected parent of a destructuring declaration entry: " + parentClass);
        }

        return (KtDestructuringDeclaration) parent;
    }

    @Override
    public PsiElement getValOrVarKeyword() {
        ASTNode node = getDestructuringDeclaration().getNode().findChildByType(KtTokens.VAL_VAR);
        if (node == null) return null;
        return node.getPsi();
    }

    /**
     * @return the PSI element for the val or var keyword of the entry itself or null.
     *
     * Only entries of full form destructuring declarations have their own val or var keyword.
     */
    @Nullable
    public PsiElement getOwnValOrVarKeyword() {
        return findChildByType(KtTokens.VAL_VAR);
    }

    /** Always {@code null}: a destructuring entry has no fully qualified name. */
    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtElement enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock instanceof KtParameter) {
            enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration((KtParameter) enclosingBlock, false);
        }
        if (enclosingBlock != null) return new LocalSearchScope(enclosingBlock);

        // Not KtNamedDeclarationStub.getUseScope: its rules are for declarations that can be private or class members
        return ResolveScopeManager.getElementUseScope(this);
    }
}
