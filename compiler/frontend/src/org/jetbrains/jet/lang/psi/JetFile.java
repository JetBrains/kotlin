/*
 * @author max
 */
package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.List;

public class JetFile extends PsiFileBase {
    public JetFile(FileViewProvider viewProvider) {
        super(viewProvider, JetLanguage.INSTANCE);
    }

    @NotNull
    public FileType getFileType() {
        return JetFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "JetFile: " + getName();
    }

    public List<JetDeclaration> getDeclarations() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDeclaration.class);
    }

    public List<JetImportDirective> getImportDirectives() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetImportDirective.class);
    }

    @NotNull
    public JetNamespaceHeader getNamespaceHeader() {
        return (JetNamespaceHeader) getNode().findChildByType(JetNodeTypes.NAMESPACE_HEADER).getPsi();
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
}
