/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code package} directive that declares the package of a Kotlin file.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    package com.example.myapp
 * // ^_______________________^
 * }</pre>
 */
public class KtPackageDirective extends KtModifierListOwnerStub<KotlinPlaceHolderStub<KtPackageDirective>> {
    private String qualifiedNameCache = null;

    public KtPackageDirective(@NotNull ASTNode node) {
        super(node);
    }

    public KtPackageDirective(@NotNull KotlinPlaceHolderStub<KtPackageDirective> stub) {
        super(stub, KtStubBasedElementTypes.PACKAGE_DIRECTIVE);
    }

    /**
     * Returns the expression denoting the package name, or {@code null} for the root package. This is either a
     * {@link KtSimpleNameExpression} (a single-segment package) or a {@link KtDotQualifiedExpression} (a dotted name).
     */
    @Nullable
    public KtExpression getPackageNameExpression() {
        return KtStubbedPsiUtil.getStubOrPsiChild(this, KtTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, KtExpression.ARRAY_FACTORY);
    }

    /**
     * Returns the individual name segments of the package, from the outermost to the innermost (for example,
     * {@code com}, {@code example}, {@code myapp}). Returns an empty list for the root package.
     */
    @NotNull
    public List<KtSimpleNameExpression> getPackageNames() {
        KtExpression nameExpression = getPackageNameExpression();
        if (nameExpression == null) return Collections.emptyList();

        List<KtSimpleNameExpression> packageNames = new ArrayList<>();
        while (nameExpression instanceof KtQualifiedExpression) {
            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) nameExpression;

            KtExpression selector = qualifiedExpression.getSelectorExpression();
            if (selector instanceof KtSimpleNameExpression) {
                packageNames.add((KtSimpleNameExpression) selector);
            }

            nameExpression = qualifiedExpression.getReceiverExpression();
        }

        if (nameExpression instanceof KtSimpleNameExpression) {
            packageNames.add((KtSimpleNameExpression) nameExpression);
        }

        Collections.reverse(packageNames);

        return packageNames;
    }

    /**
     * Returns the last (innermost) name segment of the package, or {@code null} for the root package.
     */
    @Nullable
    public KtSimpleNameExpression getLastReferenceExpression() {
        KtExpression nameExpression = getPackageNameExpression();
        if (nameExpression == null) return null;

        return (KtSimpleNameExpression) KtPsiUtilKt.getQualifiedElementSelector(nameExpression);
    }

    /**
     * Returns the identifier token of the last name segment, or {@code null} for the root package.
     */
    @Nullable
    public PsiElement getNameIdentifier() {
        KtSimpleNameExpression lastPart = getLastReferenceExpression();
        return lastPart != null ? lastPart.getIdentifier() : null;
    }

    @Override
    @NotNull
    public String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? "" : nameIdentifier.getText();
    }

    /**
     * Returns the last name segment as a {@link Name}, or {@link SpecialNames#ROOT_PACKAGE} for the root package.
     */
    @NotNull
    public Name getNameAsName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? SpecialNames.ROOT_PACKAGE : Name.identifier(nameIdentifier.getText());
    }

    /**
     * Returns {@code true} if this directive declares the root package (an empty or absent package name).
     */
    public boolean isRoot() {
        return getName().length() == 0;
    }

    /**
     * Returns the fully qualified package name, or {@link FqName#ROOT} for the root package.
     */
    @NotNull
    public FqName getFqName() {
        String qualifiedName = getQualifiedName();
        return qualifiedName.isEmpty() ? FqName.ROOT : new FqName(qualifiedName);
    }

    /**
     * Returns the fully qualified name of the package up to and including the given name segment.
     *
     * <p>This is useful for resolving a click on an intermediate segment of a dotted package name to the corresponding
     * partial package.
     */
    @NotNull
    public FqName getFqName(KtSimpleNameExpression nameExpression) {
        return new FqName(getQualifiedNameOf(nameExpression));
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.setPackageFqName(this, fqName)}
     * instead.
     */
    @Deprecated
    public void setFqName(@NotNull FqName fqName) {
        KtPsiMutationService.getInstance().setPackageFqName(this, fqName);
    }

    /**
     * Returns the dot-separated package name as a string, or an empty string for the root package. The result is
     * cached.
     */
    @NotNull
    public String getQualifiedName() {
        if (qualifiedNameCache == null) {
            qualifiedNameCache = getQualifiedNameOf(null);
        }

        return qualifiedNameCache;
    }

    @NotNull
    private String getQualifiedNameOf(@Nullable KtSimpleNameExpression nameExpression) {
        StringBuilder builder = new StringBuilder();
        for (KtSimpleNameExpression e : getPackageNames()) {
            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append(e.getReferencedName());

            if (e == nameExpression) break;
        }
        return builder.toString();
    }

    /**
     * Returns the {@code package} keyword token, or {@code null} if it is absent (for an implicit root package).
     */
    @Nullable
    public PsiElement getPackageKeyword() {
        return findChildByType(KtTokens.PACKAGE_KEYWORD);
    }

    @Override
    public void subtreeChanged() {
        qualifiedNameCache = null;
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPackageDirective(this, data);
    }
}
