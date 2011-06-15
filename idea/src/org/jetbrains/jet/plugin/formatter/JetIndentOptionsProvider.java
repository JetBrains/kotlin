package org.jetbrains.jet.plugin.formatter;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.FileTypeIndentOptionsProvider;
import org.jetbrains.jet.plugin.JetFileType;

/**
 * @author yole
 */
public class JetIndentOptionsProvider implements FileTypeIndentOptionsProvider {
    @Override
    public CodeStyleSettings.IndentOptions createIndentOptions() {
        return new CodeStyleSettings.IndentOptions();
    }

    @Override
    public FileType getFileType() {
        return JetFileType.INSTANCE;
    }

    @Override
    public IndentOptionsEditor createOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public String getPreviewText() {
        return "";
    }

    @Override
    public void prepareForReformat(PsiFile psiFile) {
    }
}
