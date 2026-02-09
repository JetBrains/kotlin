/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.NameUtils;
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub;

import java.util.List;

/**
 * Represents a Kotlin script file containing top-level statements and declarations.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * // script.kts
 * val x = 1
 * println(x)
 * }</pre>
 */
public class KtScript extends KtNamedDeclarationStub<KotlinScriptStub> implements KtDeclarationContainer {
    public KtScript(@NotNull ASTNode node) {
        super(node);
    }

    public KtScript(@NotNull KotlinScriptStub stub) {
        super(stub, KtStubBasedElementTypes.SCRIPT);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        KotlinScriptStub stub = getGreenStub();
        if (stub != null) {
            return stub.getFqName();
        }
        KtFile containingKtFile = getContainingKtFile();
        Name fileBasedName = NameUtils.getScriptNameForFile(containingKtFile.getName());
        return containingKtFile.getPackageFqName().child(fileBasedName);
    }

    @Override
    public String getName() {
        return getFqName().shortName().asString();
    }

    @NotNull
    public KtBlockExpression getBlockExpression() {
        return findNotNullChildByClass(KtBlockExpression.class);
    }

    @Override
    @NotNull
    public List<KtDeclaration> getDeclarations() {
        return PsiTreeUtil.getChildrenOfTypeAsList(getBlockExpression(), KtDeclaration.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitScript(this, data);
    }
}
