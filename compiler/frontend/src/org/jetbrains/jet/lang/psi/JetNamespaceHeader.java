/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;

import java.util.List;

public class JetNamespaceHeader extends JetReferenceExpression {
    private String qualifiedNameCache = null;

    public JetNamespaceHeader(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public List<JetSimpleNameExpression> getParentNamespaceNames() {
        List<JetSimpleNameExpression> parentParts = findChildrenByType(JetNodeTypes.REFERENCE_EXPRESSION);
        JetSimpleNameExpression lastPart = (JetSimpleNameExpression)findLastChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
        parentParts.remove(lastPart);
        return parentParts;
    }

    @Nullable
    public JetSimpleNameExpression getLastPartExpression() {
        return (JetSimpleNameExpression)findLastChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable
    public PsiElement getNameIdentifier() {
        JetSimpleNameExpression lastPart = (JetSimpleNameExpression)findLastChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
        if (lastPart == null) {
            return null;
        }

        return lastPart.getIdentifier();
    }

    @Override
    @NotNull
    public String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? "" : nameIdentifier.getText();
    }

    @NotNull
    public Name getNameAsName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? SpecialNames.ROOT_NAMESPACE : Name.identifier(nameIdentifier.getText());
    }

    public boolean isRoot() {
        return getName().length() == 0;
    }

    @NotNull
    public FqName getFqName() {
        String qualifiedName = getQualifiedName();
        return qualifiedName.isEmpty() ? FqName.ROOT : new FqName(qualifiedName);
    }

    @NotNull
    public FqName getParentFqName(JetSimpleNameExpression nameExpression) {
        String parentQualifiedName = getQualifiedNameParentOf(nameExpression);
        return parentQualifiedName.isEmpty() ? FqName.ROOT : new FqName(parentQualifiedName);
    }

    @NotNull
    public String getQualifiedName() {
        if (qualifiedNameCache == null) {
            qualifiedNameCache = getQualifiedNameParentOf(null);
        }

        return qualifiedNameCache;
    }

    @NotNull
    private String getQualifiedNameParentOf(@Nullable JetSimpleNameExpression nameExpression) {
        StringBuilder builder = new StringBuilder();
        for (JetSimpleNameExpression e : findChildrenByClass(JetSimpleNameExpression.class)) {
            if (e == nameExpression) {
                break;
            }

            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append(e.getReferencedName());
        }
        return builder.toString();
    }

    @Override
    public void subtreeChanged() {
        qualifiedNameCache = null;
    }
}

