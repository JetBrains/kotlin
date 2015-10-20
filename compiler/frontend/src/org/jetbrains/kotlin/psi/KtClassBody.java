/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.*;

public class KtClassBody extends KtElementImplStub<KotlinPlaceHolderStub<KtClassBody>> implements KtDeclarationContainer {

    public KtClassBody(@NotNull ASTNode node) {
        super(node);
    }

    public KtClassBody(@NotNull KotlinPlaceHolderStub<KtClassBody> stub) {
        super(stub, CLASS_BODY);
    }

    @Override
    @NotNull
    public List<KtDeclaration> getDeclarations() {
        return Arrays.asList(getStubOrPsiChildren(DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY));
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitClassBody(this, data);
    }

    @NotNull
    public List<KtClassInitializer> getAnonymousInitializers() {
        return findChildrenByType(KtNodeTypes.ANONYMOUS_INITIALIZER);
    }

    @NotNull
    /* package-protected */ List<KtSecondaryConstructor> getSecondaryConstructors() {
        return getStubOrPsiChildrenAsList(KtStubElementTypes.SECONDARY_CONSTRUCTOR);
    }

    @NotNull
    public List<KtProperty> getProperties() {
        return getStubOrPsiChildrenAsList(KtStubElementTypes.PROPERTY);
    }

    @NotNull
    public List<KtObjectDeclaration> getAllCompanionObjects() {
        List<KtObjectDeclaration> result = Lists.newArrayList();
        for (KtObjectDeclaration declaration : getStubOrPsiChildrenAsList(KtStubElementTypes.OBJECT_DECLARATION)) {
            if (declaration.isCompanion()) {
                result.add(declaration);
            }
        }
        return result;
    }

    @Nullable
    public PsiElement getRBrace() {
        ASTNode[] children = getNode().getChildren(TokenSet.create(KtTokens.RBRACE));
        return children.length == 1 ? children[0].getPsi() : null;
    }

    @Nullable
    public PsiElement getLBrace() {
        ASTNode[] children = getNode().getChildren(TokenSet.create(KtTokens.LBRACE));
        return children.length == 1 ? children[0].getPsi() : null;
    }

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    @NotNull
    public List<KtAnnotationEntry> getDanglingAnnotations() {
        return CollectionsKt.flatMap(
                getStubOrPsiChildrenAsList(MODIFIER_LIST),
                new Function1<KtModifierList, Iterable<KtAnnotationEntry>>() {
                    @Override
                    public Iterable<KtAnnotationEntry> invoke(KtModifierList modifierList) {
                        return modifierList.getAnnotationEntries();
                    }
                });
    }
}
