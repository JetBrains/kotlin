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

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinParameterStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersKt;

import java.util.Collections;
import java.util.List;

public class KtParameter extends KtNamedDeclarationStub<KotlinParameterStub> implements KtCallableDeclaration, KtValVarKeywordOwner {

    public KtParameter(@NotNull ASTNode node) {
        super(node);
    }

    public KtParameter(@NotNull KotlinParameterStub stub) {
        super(stub, KtStubElementTypes.VALUE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitParameter(this, data);
    }

    @Override
    @Nullable
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @Override
    @Nullable
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        return TypeRefHelpersKt.setTypeReference(this, getNameIdentifier(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(KtTokens.COLON);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    public boolean hasDefaultValue() {
        KotlinParameterStub stub = getStub();
        if (stub != null) {
            return stub.hasDefaultValue();
        }
        return getDefaultValue() != null;
    }

    @Nullable
    public KtExpression getDefaultValue() {
        KotlinParameterStub stub = getStub();
        if (stub != null && !stub.hasDefaultValue()) return null;

        PsiElement equalsToken = getEqualsToken();
        return equalsToken != null ? PsiTreeUtil.getNextSiblingOfType(equalsToken, KtExpression.class) : null;
    }

    public boolean isMutable() {
        KotlinParameterStub stub = getStub();
        if (stub != null) {
            return stub.isMutable();
        }

        return findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    public boolean isVarArg() {
        KtModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(KtTokens.VARARG_KEYWORD);
    }

    public boolean hasValOrVar() {
        KotlinParameterStub stub = getStub();
        if (stub != null) {
            return stub.hasValOrVar();
        }
        return getValOrVarKeyword() != null;
    }

    @Nullable
    public PsiElement getValOrVarKeyword() {
        KotlinParameterStub stub = getStub();
        if (stub != null && !stub.hasValOrVar()) {
            return null;
        }
        return findChildByType(VAL_VAR_TOKEN_SET);
    }

    @Nullable
    public KtDestructuringDeclaration getDestructuringDeclaration() {
        // No destructuring declaration in stubs
        if (getStub() != null) return null;

        return findChildByType(KtNodeTypes.DESTRUCTURING_DECLARATION);
    }

    public static final TokenSet VAL_VAR_TOKEN_SET = TokenSet.create(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD);

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    public boolean isLoopParameter() {
        return getParent() instanceof KtForExpression;
    }

    private <T extends PsiElement> boolean checkParentOfParentType(Class<T> klass) {
        // `parent` is supposed to be [KtParameterList]
        PsiElement parent = getParent();
        if (parent == null) {
            return false;
        }
        return klass.isInstance(parent.getParent());
    }

    public boolean isCatchParameter() {
        return checkParentOfParentType(KtCatchClause.class);
    }

    /**
     * For example,
     *   lambdaConsumer { lambdaParameter ->
     *     ...
     *   }
     *
     * @return [true] if this [KtParameter] is a parameter of a lambda.
     */
    public boolean isLambdaParameter() {
        return checkParentOfParentType(KtFunctionLiteral.class);
    }

    /**
     * For example,
     *   fun foo(lambdaArgument: (functionTypeParameter: T, ...) -> R) { ... }
     *
     * @return [true] if this [KtParameter] is a parameter of a function type.
     */
    public boolean isFunctionTypeParameter() {
        return checkParentOfParentType(KtFunctionType.class);
    }

    @Nullable
    @Override
    public KtParameterList getValueParameterList() {
        return null;
    }

    @NotNull
    @Override
    public List<KtParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public KtTypeReference getReceiverTypeReference() {
        return null;
    }

    @NotNull
    @Override
    public List<KtTypeReference> getContextReceiverTypeReferences() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public KtTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public KtTypeConstraintList getTypeConstraintList() {
        return null;
    }

    @NotNull
    @Override
    public List<KtTypeConstraint> getTypeConstraints() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<KtTypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    @Nullable
    public KtDeclarationWithBody getOwnerFunction() {
        PsiElement parent = getParentByStub();
        if (!(parent instanceof KtParameterList)) return null;
        return ((KtParameterList) parent).getOwnerFunction();
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtExpression owner = getOwnerFunction();
        if (owner instanceof KtPrimaryConstructor) {
            if (hasValOrVar()) return super.getUseScope();
            owner = ((KtPrimaryConstructor) owner).getContainingClassOrObject();
        }
        if (owner == null) {
            owner = PsiTreeUtil.getParentOfType(this, KtExpression.class);
        }
        return new LocalSearchScope(owner != null ? owner : this);
    }
}
