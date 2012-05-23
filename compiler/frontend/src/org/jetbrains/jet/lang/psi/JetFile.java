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

/*
 * @author max
 */
package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubTree;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.List;

public class JetFile extends PsiFileBase implements JetDeclarationContainer {
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

    // scripts has no namespace header
    @Nullable
    public JetNamespaceHeader getNamespaceHeader() {
        ASTNode ast = getNode().findChildByType(JetNodeTypes.NAMESPACE_HEADER);
        return ast != null ? (JetNamespaceHeader) ast.getPsi() : null;
    }

    @Nullable
    public JetScript getScript() {
        return PsiTreeUtil.getChildOfType(this, JetScript.class);
    }

    public boolean isScript() {
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
            ((JetVisitorVoid) visitor).visitJetFile(this);
        }
        else {
            visitor.visitFile(this);
        }
    }

    @Override
    public StubElement getStub() {
        return super.getStub();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public StubTree calcStubTree() {
        return super.calcStubTree();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public StubTree getStubTree() {
        return super.getStubTree();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
