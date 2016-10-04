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
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import static org.jetbrains.kotlin.psi.KtPsiFactoryKt.KtPsiFactory;

abstract class KtNamedDeclarationStub<T extends KotlinStubWithFqName<?>> extends KtDeclarationStub<T> implements KtNamedDeclaration {
    public KtNamedDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtNamedDeclarationStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        T stub = getStub();
        if (stub != null) {
            return stub.getName();
        }

        PsiElement identifier = getNameIdentifier();
        if (identifier != null) {
            String text = identifier.getText();
            return text != null ? KtPsiUtil.unquoteIdentifier(text) : null;
        }
        else {
            return null;
        }
    }

    @Override
    public Name getNameAsName() {
        String name = getName();
        return name != null ? Name.identifier(name) : null;
    }

    @Override
    @NotNull
    public Name getNameAsSafeName() {
        return KtPsiUtil.safeName(getName());
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(KtTokens.IDENTIFIER);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        PsiElement identifier = getNameIdentifier();
        if (identifier == null) return null;

        KtModifierList modifierList = getModifierList();
        if (modifierList != null &&
            modifierList.hasModifier(KtTokens.OPERATOR_KEYWORD) &&
            !OperatorConventions.isConventionName(Name.identifier(name))) {
            removeModifier(KtTokens.OPERATOR_KEYWORD);
        }

        return identifier.replace(KtPsiFactory(this).createNameIdentifier(name));
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtElement enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock != null) {
            PsiElement enclosingParent = enclosingBlock.getParent();
            if (enclosingParent instanceof KtContainerNode) {
                enclosingParent = enclosingParent.getParent();
            }
            if (enclosingBlock instanceof KtBlockExpression && enclosingParent instanceof KtDoWhileExpression) {
                KtExpression condition = ((KtDoWhileExpression) enclosingParent).getCondition();
                if (condition != null) return new LocalSearchScope(new PsiElement[] { enclosingBlock, condition });
            }

            return new LocalSearchScope(enclosingBlock);
        }

        if (hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            KtElement containingClass = PsiTreeUtil.getParentOfType(this, KtClassOrObject.class);
            if (containingClass instanceof KtObjectDeclaration && ((KtObjectDeclaration) containingClass).isCompanion()) {
                KtElement companionObjectClass = PsiTreeUtil.getParentOfType(containingClass, KtClassOrObject.class);
                if (companionObjectClass != null) {
                    containingClass = companionObjectClass;
                }
            }
            if (containingClass != null) {
                return new LocalSearchScope(containingClass);
            }
        }

        SearchScope scope = super.getUseScope();

        KtClassOrObject classOrObject = KtPsiUtilKt.getContainingClassOrObject(this);
        if (classOrObject != null) {
            scope = scope.intersectWith(classOrObject.getUseScope());
        }

        return scope;
    }

    @Nullable
    @Override
    public FqName getFqName() {
        // TODO: stubs do not agree with PSI here in case where there's no name:
        // stubs return a normalized name, and PSI returns null
        T stub = getStub();
        if (stub != null) {
            return stub.getFqName();
        }
        return KtNamedDeclarationUtil.getFQName(this);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        KtClassOrObject classOrObject = KtPsiUtilKt.getContainingClassOrObject(this);

        super.delete();

        if (classOrObject instanceof KtObjectDeclaration
            && ((KtObjectDeclaration) classOrObject).isCompanion()
            && classOrObject.getDeclarations().isEmpty()) {
            classOrObject.delete();
        }
    }
}
