package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public interface DiagnosticFactory {
    @NotNull
    TextRange getMarkerPosition(@NotNull Diagnostic diagnostic);
}
