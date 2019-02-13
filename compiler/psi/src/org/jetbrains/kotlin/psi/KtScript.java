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
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.NameUtils;
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.List;

public class KtScript extends KtNamedDeclarationStub<KotlinScriptStub> implements KtDeclarationContainer {
    public KtScript(@NotNull ASTNode node) {
        super(node);
    }

    public KtScript(@NotNull KotlinScriptStub stub) {
        super(stub, KtStubElementTypes.SCRIPT);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        KotlinScriptStub stub = getStub();
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
