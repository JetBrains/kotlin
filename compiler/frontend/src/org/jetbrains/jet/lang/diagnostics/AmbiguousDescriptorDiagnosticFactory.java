package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;

/**
* @author abreslav
*/
public class AmbiguousDescriptorDiagnosticFactory extends ParameterizedDiagnosticFactory1<Collection<? extends ResolvedCall<? extends DeclarationDescriptor>>> {
    public static AmbiguousDescriptorDiagnosticFactory create(String messageTemplate) {
        return new AmbiguousDescriptorDiagnosticFactory(messageTemplate);
    }

    public AmbiguousDescriptorDiagnosticFactory(String messageTemplate) {
        super(Severity.ERROR, messageTemplate);
    }

    @Override
    protected String makeMessageFor(@NotNull Collection<? extends ResolvedCall<? extends DeclarationDescriptor>> argument) {
        StringBuilder stringBuilder = new StringBuilder("\n");
        for (ResolvedCall<? extends DeclarationDescriptor> call : argument) {
            stringBuilder.append(DescriptorRenderer.TEXT.render(call.getResultingDescriptor())).append("\n");
        }
        return stringBuilder.toString();
    }
}
