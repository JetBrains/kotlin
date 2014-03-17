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

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.Collections;
import java.util.List;

public class JetFile extends PsiFileBase implements JetDeclarationContainer, JetElement, PsiClassOwner {

    private final boolean isCompiled;

    public JetFile(FileViewProvider viewProvider, boolean compiled) {
        super(viewProvider, JetLanguage.INSTANCE);
        this.isCompiled = compiled;
    }

    @Override
    public FileASTNode getNode() {
        return super.getNode();
    }

    public boolean isCompiled() {
        return isCompiled;
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

    @Nullable
    public JetImportList getImportList() {
        return findChildByClass(JetImportList.class);
    }

    @NotNull
    public List<JetImportDirective> getImportDirectives() {
        JetImportList importList = getImportList();
        return importList != null ? importList.getImports() : Collections.<JetImportDirective>emptyList();
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

    // scripts have no package directive
    @Nullable
    public JetPackageDirective getPackageDirective() {
        ASTNode ast = getNode().findChildByType(JetNodeTypes.PACKAGE_DIRECTIVE);
        return ast != null ? (JetPackageDirective) ast.getPsi() : null;
    }

    @Override
    @Nullable
    public String getPackageName() {
        PsiJetFileStub stub = (PsiJetFileStub) getStub();
        if (stub != null) {
            return stub.getPackageName();
        }

        JetPackageDirective directive = getPackageDirective();
        return directive != null ? directive.getQualifiedName() : null;
    }

    @NotNull
    @Override
    public PsiClass[] getClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @Override
    public void setPackageName(String packageName) { }

    // SCRIPT: find script in file
    @Nullable
    public JetScript getScript() {
        return PsiTreeUtil.getChildOfType(this, JetScript.class);
    }

    public boolean isScript() {
        PsiJetFileStub stub = (PsiJetFileStub) getStub();
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
        if (visitor instanceof JetVisitor) {
            accept((JetVisitor) visitor, null);
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
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitJetFile(this, data);
    }
}
