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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetImportDirectiveStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetTokens;

public class JetImportDirective extends JetElementImplStub<PsiJetImportDirectiveStub> {

    public static final TokenSet IMPORT_DIRECTIVE_EXPRESSIONS =
            TokenSet.create(JetStubElementTypes.REFERENCE_EXPRESSION, JetStubElementTypes.DOT_QUALIFIED_EXPRESSION);

    public JetImportDirective(@NotNull ASTNode node) {
        super(node);
    }

    public JetImportDirective(@NotNull PsiJetImportDirectiveStub stub) {
        super(stub, JetStubElementTypes.IMPORT_DIRECTIVE);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitImportDirective(this, data);
    }

    public boolean isAbsoluteInRootPackage() {
        PsiJetImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isAbsoluteInRootPackage();
        }
        return findChildByType(JetTokens.PACKAGE_KEYWORD) != null;
    }

    @Nullable @IfNotParsed
    public JetExpression getImportedReference() {
        JetExpression[] references = getStubOrPsiChildren(IMPORT_DIRECTIVE_EXPRESSIONS, JetExpression.ARRAY_FACTORY);
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
        PsiJetImportDirectiveStub stub = getStub();
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
        PsiJetImportDirectiveStub stub = getStub();
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
