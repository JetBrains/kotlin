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
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

public class JetNamespaceHeader extends JetReferenceExpression {
    private String qualifiedNameCache = null;

    public JetNamespaceHeader(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public List<JetSimpleNameExpression> getParentNamespaceNames() {
        List<JetSimpleNameExpression> parentParts = getPackageNameAsNameList();
        return parentParts.subList(0, parentParts.size() - 1);
    }

    @Nullable
    public JetSimpleNameExpression getLastPartExpression() {
        return (JetSimpleNameExpression)findLastChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @NotNull
    public List<JetSimpleNameExpression> getPackageNameAsNameList() {
        return findChildrenByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
    }

    @Nullable
    @Override
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        return references.length == 1 ? references[0] : null;
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
        return nameIdentifier == null ? JetPsiUtil.ROOT_NAMESPACE_NAME : Name.identifier(nameIdentifier.getText());
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
    public FqName getParentFqName(JetReferenceExpression nameExpression) {
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
    private String getQualifiedNameParentOf(@Nullable JetReferenceExpression nameExpression) {
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

