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
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.resolve.ImportPath;

public class KtImportDirective extends KtElementImplStub<KotlinImportDirectiveStub> {

    public KtImportDirective(@NotNull ASTNode node) {
        super(node);
    }

    public KtImportDirective(@NotNull KotlinImportDirectiveStub stub) {
        super(stub, KtStubElementTypes.IMPORT_DIRECTIVE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitImportDirective(this, data);
    }

    @Nullable
    @IfNotParsed
    public KtExpression getImportedReference() {
        KtExpression[] references = getStubOrPsiChildren(KtStubElementTypes.INSIDE_DIRECTIVE_EXPRESSIONS, KtExpression.ARRAY_FACTORY);
        if (references.length > 0) {
            return references[0];
        }
        return null;
    }

    @Nullable
    public KtImportAlias getAlias() {
        return getStubOrPsiChild(KtStubElementTypes.IMPORT_ALIAS);
    }

    @Nullable
    public String getAliasName() {
        KtImportAlias alias = getAlias();
        return alias != null ? alias.getName() : null;
    }

    public boolean isAllUnder() {
        KotlinImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isAllUnder();
        }
        return getNode().findChildByType(KtTokens.MUL) != null;
    }

    @Nullable
    @IfNotParsed
    public FqName getImportedFqName() {
        KotlinImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.getImportedFqName();
        }
        return fqNameFromExpression(getImportedReference());
    }

    @Nullable
    @IfNotParsed
    public ImportPath getImportPath() {
        FqName importFqn = getImportedFqName();
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
    private static FqName fqNameFromExpression(@Nullable KtExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof KtDotQualifiedExpression) {
            KtDotQualifiedExpression dotQualifiedExpression = (KtDotQualifiedExpression) expression;
            FqName parentFqn = fqNameFromExpression(dotQualifiedExpression.getReceiverExpression());
            Name child = nameFromExpression(dotQualifiedExpression.getSelectorExpression());
            if (child == null) {
                return parentFqn;
            }
            if (parentFqn != null) {
                return parentFqn.child(child);
            }
            return null;
        }
        else if (expression instanceof KtSimpleNameExpression) {
            KtSimpleNameExpression simpleNameExpression = (KtSimpleNameExpression) expression;
            return FqName.topLevel(simpleNameExpression.getReferencedNameAsName());
        }
        else {
            throw new IllegalArgumentException("Can't construct fqn for: " + expression.getClass().toString());
        }
    }

    @Nullable
    private static Name nameFromExpression(@Nullable KtExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof KtSimpleNameExpression) {
            return ((KtSimpleNameExpression) expression).getReferencedNameAsName();
        }
        else {
            throw new IllegalArgumentException("Can't construct name for: " + expression.getClass().toString());
        }
    }
}
