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
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;

import java.util.Collections;
import java.util.List;

public class JetPackageDirective extends JetModifierListOwnerStub<KotlinPlaceHolderStub<JetPackageDirective>> implements JetReferenceExpression {
    private String qualifiedNameCache = null;

    public JetPackageDirective(@NotNull ASTNode node) {
        super(node);
    }

    public JetPackageDirective(@NotNull KotlinPlaceHolderStub<JetPackageDirective> stub) {
        super(stub, JetStubElementTypes.PACKAGE_DIRECTIVE);
    }

    // This should be either JetSimpleNameExpression, or JetDotQualifiedExpression
    @Nullable
    public JetExpression getPackageNameExpression() {
        return JetStubbedPsiUtil.getStubOrPsiChild(this, JetStubElementTypes.INSIDE_DIRECTIVE_EXPRESSIONS, JetExpression.ARRAY_FACTORY);
    }

    @NotNull
    public List<JetSimpleNameExpression> getPackageNames() {
        JetExpression nameExpression = getPackageNameExpression();
        if (nameExpression == null) return Collections.emptyList();

        List<JetSimpleNameExpression> packageNames = ContainerUtil.newArrayList();
        while (nameExpression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) nameExpression;

            JetExpression selector = qualifiedExpression.getSelectorExpression();
            if (selector instanceof JetSimpleNameExpression) {
                packageNames.add((JetSimpleNameExpression) selector);
            }

            nameExpression = qualifiedExpression.getReceiverExpression();
        }

        if (nameExpression instanceof JetSimpleNameExpression) {
            packageNames.add((JetSimpleNameExpression) nameExpression);
        }

        Collections.reverse(packageNames);

        return packageNames;
    }

    @Nullable
    public JetSimpleNameExpression getLastReferenceExpression() {
        JetExpression nameExpression = getPackageNameExpression();
        if (nameExpression == null) return null;

        return (JetSimpleNameExpression)PsiUtilPackage.getQualifiedElementSelector(nameExpression);
    }

    @Nullable
    public PsiElement getNameIdentifier() {
        JetSimpleNameExpression lastPart = getLastReferenceExpression();
        return lastPart != null ? lastPart.getIdentifier() : null;
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
        return nameIdentifier == null ? SpecialNames.ROOT_PACKAGE : Name.identifier(nameIdentifier.getText());
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
    public FqName getFqName(JetSimpleNameExpression nameExpression) {
        return new FqName(getQualifiedNameOf(nameExpression));
    }

    @NotNull
    public String getQualifiedName() {
        if (qualifiedNameCache == null) {
            qualifiedNameCache = getQualifiedNameOf(null);
        }

        return qualifiedNameCache;
    }

    @NotNull
    private String getQualifiedNameOf(@Nullable JetSimpleNameExpression nameExpression) {
        StringBuilder builder = new StringBuilder();
        for (JetSimpleNameExpression e : getPackageNames()) {
            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append(e.getReferencedName());

            if (e == nameExpression) break;
        }
        return builder.toString();
    }

    @Override
    public void subtreeChanged() {
        qualifiedNameCache = null;
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitPackageDirective(this, data);
    }
}

