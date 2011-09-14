package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface DiagnosticWithTextRange extends Diagnostic {
    @NotNull
    TextRange getTextRange();

}
