/*
 * @author max
 */
package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

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

    @NotNull
    public JetNamespace getRootNamespace() {
        return (JetNamespace) getNode().findChildByType(JetNodeTypes.NAMESPACE).getPsi();
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            ((JetVisitor) visitor).visitJetFile(this);
        }
        else {
            visitor.visitFile(this);
        }
    }
}
