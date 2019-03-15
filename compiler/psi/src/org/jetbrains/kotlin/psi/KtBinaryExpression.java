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
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Arrays;

public class KtBinaryExpression extends KtExpressionImpl implements KtOperationExpression {
    public KtBinaryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBinaryExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getLeft() {
        ASTNode node = getOperationReference().getNode().getTreePrev();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof KtExpression) {
                return (KtExpression) psi;
            }
            node = node.getTreePrev();
        }

        return null;
    }

    @Nullable @IfNotParsed
    public KtExpression getRight() {
        ASTNode node = getOperationReference().getNode().getTreeNext();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof KtExpression) {
                return (KtExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @NotNull
    public KtOperationReferenceExpression getOperationReference() {
        PsiElement operationReference = findChildByType(KtNodeTypes.OPERATION_REFERENCE);
        if (operationReference == null) {
            throw new NullPointerException("No operation reference for binary expression: " + Arrays.toString(getChildren()));
        }

        return (KtOperationReferenceExpression) operationReference;
    }

    @NotNull
    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }
}

