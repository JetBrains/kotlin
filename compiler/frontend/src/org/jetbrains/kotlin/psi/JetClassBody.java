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
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Arrays;
import java.util.List;

import static kotlin.KotlinPackage.firstOrNull;
import static org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes.*;

public class JetClassBody extends JetElementImplStub<KotlinPlaceHolderStub<JetClassBody>> implements JetDeclarationContainer {

    public JetClassBody(@NotNull ASTNode node) {
        super(node);
    }

    public JetClassBody(@NotNull KotlinPlaceHolderStub<JetClassBody> stub) {
        super(stub, CLASS_BODY);
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        return Arrays.asList(getStubOrPsiChildren(DECLARATION_TYPES, JetDeclaration.ARRAY_FACTORY));
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitClassBody(this, data);
    }

    @NotNull
    public List<JetClassInitializer> getAnonymousInitializers() {
        return findChildrenByType(JetNodeTypes.ANONYMOUS_INITIALIZER);
    }

    @NotNull
    /* package-protected */ List<JetSecondaryConstructor> getSecondaryConstructors() {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.SECONDARY_CONSTRUCTOR);
    }

    @NotNull
    public List<JetProperty> getProperties() {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.PROPERTY);
    }

    @NotNull
    public List<JetObjectDeclaration> getAllDefaultObjects() {
        List<JetObjectDeclaration> result = Lists.newArrayList();
        for (JetObjectDeclaration declaration : getStubOrPsiChildrenAsList(JetStubElementTypes.OBJECT_DECLARATION)) {
            if (declaration.isDefault()) {
                result.add(declaration);
            }
        }
        return result;
    }

    @Nullable
    public PsiElement getRBrace() {
        ASTNode[] children = getNode().getChildren(TokenSet.create(JetTokens.RBRACE));
        return children.length == 1 ? children[0].getPsi() : null;
    }

    @Nullable
    public PsiElement getLBrace() {
        ASTNode[] children = getNode().getChildren(TokenSet.create(JetTokens.LBRACE));
        return children.length == 1 ? children[0].getPsi() : null;
    }

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    @NotNull
    public List<JetAnnotationEntry> getDanglingAnnotations() {
        return KotlinPackage.flatMap(
                getStubOrPsiChildrenAsList(MODIFIER_LIST),
                new Function1<JetModifierList, Iterable<JetAnnotationEntry>>() {
                    @Override
                    public Iterable<JetAnnotationEntry> invoke(JetModifierList modifierList) {
                        return modifierList.getAnnotationEntries();
                    }
                });
    }
}
