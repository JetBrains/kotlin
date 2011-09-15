package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;

/**
* @author abreslav
*/
public class AmbiguousDescriptorDiagnosticFactory extends ParameterizedDiagnosticFactory1<Collection<? extends CallableDescriptor>> {
    public static AmbiguousDescriptorDiagnosticFactory create(String messageTemplate) {
        return new AmbiguousDescriptorDiagnosticFactory(messageTemplate);
    }

    public AmbiguousDescriptorDiagnosticFactory(String messageTemplate) {
        super(Severity.ERROR, messageTemplate);
    }

    @Override
    protected String makeMessageFor(@NotNull Collection<? extends CallableDescriptor> argument) {
        StringBuilder stringBuilder = new StringBuilder("\n");
        for (CallableDescriptor descriptor : argument) {
            stringBuilder.append(DescriptorRenderer.TEXT.render(descriptor)).append("\n");
        }
        return stringBuilder.toString();
    }
}
