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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.lexer.JetTokens;

public class JetImportDirective extends JetElementImplStub<KotlinImportDirectiveStub> {

    public JetImportDirective(@NotNull ASTNode node) {
        super(node);
    }

    public JetImportDirective(@NotNull KotlinImportDirectiveStub stub) {
        super(stub, JetStubElementTypes.IMPORT_DIRECTIVE);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitImportDirective(this, data);
    }

    public boolean isAbsoluteInRootPackage() {
        KotlinImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isAbsoluteInRootPackage();
        }
        return findChildByType(JetTokens.PACKAGE_KEYWORD) != null;
    }

    @Nullable @IfNotParsed
    public JetExpression getImportedReference() {
        JetExpression[] references = getStubOrPsiChildren(JetStubElementTypes.INSIDE_DIRECTIVE_EXPRESSIONS, JetExpression.ARRAY_FACTORY);
        if (references.length > 0) {
            return references[0];
        }
        return null;
    }

    @Nullable
    public ASTNode getAliasNameNode() {
        boolean asPassed = false;
        ASTNode childNode = getNode().getFirstChildNode();
        while (childNode != null) {
            IElementType tt = childNode.getElementType();
            if (tt == JetTokens.AS_KEYWORD) asPassed = true;
            if (asPassed && tt == JetTokens.IDENTIFIER) {
                return childNode;
            }

            childNode = childNode.getTreeNext();
        }
        return null;
    }

    @Nullable
    public String getAliasName() {
        KotlinImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.getAliasName();
        }
        ASTNode aliasNameNode = getAliasNameNode();
        if (aliasNameNode == null) {
            return null;
        }
        return aliasNameNode.getText();
    }

    public boolean isAllUnder() {
        KotlinImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isAllUnder();
        }
        return getNode().findChildByType(JetTokens.MUL) != null;
    }

    @Nullable
    @IfNotParsed
    public ImportPath getImportPath() {
        if (!isValidImport()) return null;

        FqName importFqn = fqNameFromExpression(getImportedReference());
        if (importFqn == null) {
            return null;
        }

        Name alias = null;
        String aliasName = getAliasName();
        if (aliasName != null) {
            alias = Name.identifier(aliasName);
        }

        return new ImportPath(importFqn, isAllUnder(), alias);
    }

    public boolean isValidImport() {
        KotlinImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isValid();
        }
        return !PsiTreeUtil.hasErrorElements(this);
    }

    @Nullable
    private static FqName fqNameFromExpression(@Nullable JetExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof JetDotQualifiedExpression) {
            JetDotQualifiedExpression dotQualifiedExpression = (JetDotQualifiedExpression) expression;
            FqName parentFqn = fqNameFromExpression(dotQualifiedExpression.getReceiverExpression());
            Name child = nameFromExpression(dotQualifiedExpression.getSelectorExpression());

            return parentFqn != null && child != null ? parentFqn.child(child) : null;
        }
        else if (expression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
            return FqName.topLevel(simpleNameExpression.getReferencedNameAsName());
        }
        else {
            throw new IllegalArgumentException("Can't construct fqn for: " + expression.getClass().toString());
        }
    }

    @Nullable
    private static Name nameFromExpression(@Nullable JetExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof JetSimpleNameExpression) {
            return ((JetSimpleNameExpression) expression).getReferencedNameAsName();
        }
        else {
            throw new IllegalArgumentException("Can't construct name for: " + expression.getClass().toString());
        }
    }
}
