/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.List;

public class JetFile extends PsiFileBase implements JetDeclarationContainer, JetElement {
    public JetFile(FileViewProvider viewProvider) {
        super(viewProvider, JetLanguage.INSTANCE);
    }

    @Override
    @NotNull
    public FileType getFileType() {
        return JetFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "JetFile: " + getName();
    }

    @NotNull
    @Override
    public List<JetDeclaration> getDeclarations() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDeclaration.class);
    }

    public List<JetImportDirective> getImportDirectives() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetImportDirective.class);
    }

    @Nullable
    public JetImportDirective findImportByAlias(@NotNull String name) {
        for (JetImportDirective directive : getImportDirectives()) {
            if (name.equals(directive.getAliasName())) {
                return directive;
            }
        }
        return null;
    }

    // scripts has no namespace header
    @Nullable
    public JetNamespaceHeader getNamespaceHeader() {
        ASTNode ast = getNode().findChildByType(JetNodeTypes.NAMESPACE_HEADER);
        return ast != null ? (JetNamespaceHeader) ast.getPsi() : null;
    }

    @Nullable
    public String getPackageName() {
        PsiJetFileStub stub = (PsiJetFileStub)getStub();
        if (stub != null) {
            return stub.getPackageName();
        }

        JetNamespaceHeader statement = getNamespaceHeader();
        return statement != null ? statement.getQualifiedName() : null;
    }

    @Nullable
    public JetScript getScript() {
        return PsiTreeUtil.getChildOfType(this, JetScript.class);
    }

    public boolean isScript() {
        PsiJetFileStub stub = (PsiJetFileStub)getStub();
        if (stub != null) {
            return stub.isScript();
        }

        return getScript() != null;
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName(); // TODO
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitorVoid) {
            accept((JetVisitorVoid) visitor);
        }
        else {
            visitor.visitFile(this);
        }
    }

    @Override
    public <D> void acceptChildren(@NotNull JetTreeVisitor<D> visitor, D data) {
        JetPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitJetFile(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitJetFile(this, data);
    }
}
