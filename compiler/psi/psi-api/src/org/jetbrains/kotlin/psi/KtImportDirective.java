/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;
import org.jetbrains.kotlin.resolve.ImportPath;

public class KtImportDirective extends KtElementImplStub<KotlinImportDirectiveStub> implements KtImportInfo {

    public KtImportDirective(@NotNull ASTNode node) {
        super(node);
    }

    public KtImportDirective(@NotNull KotlinImportDirectiveStub stub) {
        super(stub, KtStubBasedElementTypes.IMPORT_DIRECTIVE);
    }

    private volatile FqName importedFqName;

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitImportDirective(this, data);
    }

    @Nullable
    @IfNotParsed
    @SuppressWarnings("deprecation")
    public KtExpression getImportedReference() {
        KtExpression[] references = getStubOrPsiChildren(KtTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, KtExpression.ARRAY_FACTORY);
        if (references.length > 0) {
            return references[0];
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public KtImportAlias getAlias() {
        return getStubOrPsiChild(KtStubBasedElementTypes.IMPORT_ALIAS);
    }

    @Override
    @Nullable
    public String getAliasName() {
        KtImportAlias alias = getAlias();
        return alias != null ? alias.getName() : null;
    }

    @Override
    public boolean isAllUnder() {
        KotlinImportDirectiveStub stub = getGreenStub();
        if (stub != null) {
            return stub.isAllUnder();
        }
        return getNode().findChildByType(KtTokens.MUL) != null;
    }

    @Nullable
    @Override
    public ImportContent getImportContent() {
        KtExpression reference = getImportedReference();
        if (reference == null) return null;
        return new ImportContent.ExpressionBased(reference);
    }

    @Override
    @Nullable
    @IfNotParsed
    public FqName getImportedFqName() {
        KotlinImportDirectiveStub stub = getGreenStub();
        if (stub != null) {
            return stub.getImportedFqName();
        }

        FqName importedFqName = this.importedFqName;
        if (importedFqName != null) return importedFqName;
        KtExpression importedReference = getImportedReference();
        // in case it's not parsed
        if (importedReference == null) return null;

        importedFqName = fqNameFromExpression(importedReference);
        this.importedFqName = importedFqName;
        return importedFqName;
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
        KotlinImportDirectiveStub stub = getGreenStub();
        if (stub != null) {
            return stub.isValid();
        }
        return !PsiTreeUtil.hasErrorElements(this);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        importedFqName = null;
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
