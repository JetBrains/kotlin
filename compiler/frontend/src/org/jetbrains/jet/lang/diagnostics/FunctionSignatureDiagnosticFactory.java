package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.resolve.DescriptorRenderer;

/**
 * @author Stepan Koltsov
 */
public class FunctionSignatureDiagnosticFactory extends DiagnosticFactoryWithMessageFormat {

    public FunctionSignatureDiagnosticFactory(Severity severity, String messageTemplate) {
        super(severity, messageTemplate);
    }

    @NotNull
    public Diagnostic on(@NotNull JetNamedFunction functionElement, @NotNull FunctionDescriptor functionDescriptor,
                         @NotNull JetClassOrObject jetClassOrObject)
    {
        TextRange rangeToMark = new TextRange(
                functionElement.getStartOfSignatureElement().getTextRange().getStartOffset(),
                functionElement.getEndOfSignatureElement().getTextRange().getEndOffset()
            );
        String message = messageFormat.format(new Object[]{
                jetClassOrObject.getName(),
                DescriptorRenderer.TEXT.render(functionDescriptor)});
        return new GenericDiagnostic(this, severity, message, functionElement.getContainingFile(), rangeToMark);
    }

} //~
