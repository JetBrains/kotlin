/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

/**
 * The code example:
 * <pre>{@code
 *     @Anno
 * // ^____^
 * fun foo() {}
 * }</pre>
 */
public class KtAnnotationEntry extends KtElementImplStub<KotlinAnnotationEntryStub> implements KtCallElement {
    public KtAnnotationEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtAnnotationEntry(@NotNull KotlinAnnotationEntryStub stub) {
        super(stub, KtStubElementTypes.ANNOTATION_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotationEntry(this, data);
    }


    @Nullable @IfNotParsed
    public KtTypeReference getTypeReference() {
        KtConstructorCalleeExpression calleeExpression = getCalleeExpression();
        if (calleeExpression == null) {
            return null;
        }
        return calleeExpression.getTypeReference();
    }

    @Override
    public KtConstructorCalleeExpression getCalleeExpression() {
        return getStubOrPsiChild(KtStubElementTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public KtValueArgumentList getValueArgumentList() {
        KotlinAnnotationEntryStub stub = getStub();
        if (stub == null && getGreenStub() != null) {
            return (KtValueArgumentList) findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST);
        }

        return getStubOrPsiChild(KtStubElementTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    @Override
    public List<? extends ValueArgument> getValueArguments() {
        KotlinAnnotationEntryStub stub = getStub();
        if (stub != null && !stub.hasValueArguments()) {
            return Collections.<KtValueArgument>emptyList();
        }

        KtValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<KtValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<KtLambdaArgument> getLambdaArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<KtTypeProjection> getTypeArguments() {
        KtTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public KtTypeArgumentList getTypeArgumentList() {
        KtTypeReference typeReference = getTypeReference();
        if (typeReference == null) {
            return null;
        }
        KtTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof KtUserType) {
            KtUserType userType = (KtUserType) typeElement;
            return userType.getTypeArgumentList();
        }
        return null;
    }

    @Nullable
    public PsiElement getAtSymbol() {
        return findChildByType(KtTokens.AT);
    }

    @Nullable
    public KtAnnotationUseSiteTarget getUseSiteTarget() {
        KtAnnotationUseSiteTarget target = getStubOrPsiChild(KtStubElementTypes.ANNOTATION_TARGET);

        if (target == null) {
            PsiElement parent = getParentByStub();
            if (parent instanceof KtAnnotation) {
                return ((KtAnnotation) parent).getUseSiteTarget();
            }
        }

        return target;
    }

    @Nullable
    public Name getShortName() {
        KotlinAnnotationEntryStub stub = getStub();
        if (stub != null) {
            String shortName = stub.getShortName();
            if (shortName != null) {
                return Name.identifier(shortName);
            }
            return null;
        }

        KtTypeReference typeReference = getTypeReference();
        assert typeReference != null : "Annotation entry hasn't typeReference " + getText();
        KtTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof KtUserType) {
            KtUserType userType = (KtUserType) typeElement;
            String shortName = userType.getReferencedName();
            if (shortName != null) {
                return Name.identifier(shortName);
            }
        }
        return null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }
}
