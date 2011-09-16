package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import static org.jetbrains.jet.lang.diagnostics.Errors.REDECLARATION;

/**
 * @author abreslav
 */
public class TraceBasedRedeclarationHandler implements RedeclarationHandler {
    private final BindingTrace trace;

    public TraceBasedRedeclarationHandler(@NotNull BindingTrace trace) {
        this.trace = trace;
    }
    
    @Override
    public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        report(first);
        report(second);
    }

    private void report(DeclarationDescriptor first) {
        PsiElement firstElement = trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, first);
        if (firstElement != null) {
            trace.report(REDECLARATION.on(firstElement));
        }
        else {
            trace.report(REDECLARATION.on(first, trace.getBindingContext()));
        }
    }
}
