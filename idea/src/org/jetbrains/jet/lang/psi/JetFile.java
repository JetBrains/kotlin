/*
 * @author max
 */
package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.JetLanguage;

public class JetFile extends PsiFileBase {
    public JetFile(FileViewProvider viewProvider) {
        super(viewProvider, JetLanguage.INSTANCE);
    }

    @NotNull
    public FileType getFileType() {
        return JetFileType.INSTANCE;
    }
}
