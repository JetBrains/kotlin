package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface DiagnosticWithPsiElement<T extends PsiElement> extends DiagnosticWithTextRange {
    @NotNull
    T getPsiElement();
}
