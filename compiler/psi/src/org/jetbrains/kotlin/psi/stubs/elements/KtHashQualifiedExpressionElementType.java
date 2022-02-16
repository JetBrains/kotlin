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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtHashQualifiedExpression;

public class KtHashQualifiedExpressionElementType extends KtPlaceHolderStubElementType<KtHashQualifiedExpression> {
    public KtHashQualifiedExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtHashQualifiedExpression.class);
    }

    private static boolean checkNodeTypesTraversal(ASTNode node) {
        IElementType type = node.getElementType();
        if (type != KtStubElementTypes.HASH_QUALIFIED_EXPRESSION &&
            type != KtStubElementTypes.REFERENCE_EXPRESSION &&
            type != KtTokens.IDENTIFIER &&
            type != KtTokens.HASH
        ) {
            return false;
        }

        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (!checkNodeTypesTraversal(child)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null) return false;

        IElementType parentElementType = treeParent.getElementType();
        if (parentElementType == KtStubElementTypes.IMPORT_DIRECTIVE ||
            parentElementType == KtStubElementTypes.PACKAGE_DIRECTIVE ||
            parentElementType == KtStubElementTypes.VALUE_ARGUMENT ||
            parentElementType == KtStubElementTypes.HASH_QUALIFIED_EXPRESSION
        ) {
            return checkNodeTypesTraversal(node) && super.shouldCreateStub(node);
        }

        return false;
    }
}
