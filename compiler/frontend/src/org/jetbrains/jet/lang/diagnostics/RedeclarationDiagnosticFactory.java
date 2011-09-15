package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
* @author abreslav
*/
public class RedeclarationDiagnosticFactory implements DiagnosticFactory {

    public static final RedeclarationDiagnosticFactory INSTANCE = new RedeclarationDiagnosticFactory();

    public RedeclarationDiagnosticFactory() {}

    public RedeclarationDiagnostic on(DeclarationDescriptor a, DeclarationDescriptor b) {
        return new RedeclarationDiagnostic(a, b);
    }

    @NotNull
    @Override
    public TextRange getMarkerPosition(@NotNull Diagnostic diagnostic) {
        throw new UnsupportedOperationException(); // TODO
    }
}
