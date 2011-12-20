package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.psi.JetElement;

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
}
