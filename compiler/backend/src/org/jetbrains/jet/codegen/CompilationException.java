package org.jetbrains.jet.codegen;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.objectweb.asm.Label;

import static com.intellij.openapi.compiler.CompilerMessageCategory.ERROR;

/**
* @author alex.tkachman
*/
public class CompilationException extends RuntimeException {
    private PsiElement element;

    CompilationException(String message, Throwable cause, PsiElement element) {
        super(message, cause);
        this.element = element;
    }

    public PsiElement getElement() {
        return element;
    }

    @Override
    public String toString() {
        PsiFile psiFile = element.getContainingFile();
        TextRange textRange = element.getTextRange();
        Document document = psiFile.getViewProvider().getDocument();
        int line;
        int col;
        if (document != null) {
            line = document.getLineNumber(textRange.getStartOffset());
            col = textRange.getStartOffset() - document.getLineStartOffset(line) + 1;
        }
        else {
            line = -1;
            col = -1;
        }

        return "Internal error: (" + (line+1) + "," + col + ") " + getCause().toString();
    }
}
