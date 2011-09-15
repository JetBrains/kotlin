package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;

/**
* @author abreslav
*/
public class RedeclarationDiagnostic implements Diagnostic {

    private final DeclarationDescriptor a;
    private final DeclarationDescriptor b;

    public RedeclarationDiagnostic(DeclarationDescriptor a, DeclarationDescriptor b) {
        this.a = a;
        this.b = b;
    }

    public DeclarationDescriptor getA() {
        return a;
    }

    public DeclarationDescriptor getB() {
        return b;
    }

    @NotNull
    @Override
    public DiagnosticFactory getFactory() {
        return RedeclarationDiagnosticFactory.INSTANCE;
    }

    @NotNull
    @Override
    public String getMessage() {
        return "Redeclaration";
    }

    @NotNull
    @Override
    public Severity getSeverity() {
        return ERROR;
    }
}
