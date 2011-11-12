package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.resolve.DescriptorRenderer;

/**
 * @author Stepan Koltsov
 */
public class FunctionSignatureDiagnosticFactory extends DiagnosticFactoryWithMessageFormat {

    public FunctionSignatureDiagnosticFactory(Severity severity, String messageTemplate) {
        super(severity, messageTemplate);
    }
    
    private TextRange rangeToMark(JetDeclaration jetDeclaration) {
        if (jetDeclaration instanceof JetNamedFunction) {
            JetNamedFunction functionElement = (JetNamedFunction) jetDeclaration;
            return new TextRange(
                    functionElement.getStartOfSignatureElement().getTextRange().getStartOffset(),
                    functionElement.getEndOfSignatureElement().getTextRange().getEndOffset()
            );
        } else if (jetDeclaration instanceof JetClass) {
            // primary constructor
            JetClass klass = (JetClass) jetDeclaration;
            PsiElement nameAsDeclaration = klass.getNameIdentifier();
            PsiElement primaryConstructorParameterList = klass.getPrimaryConstructorParameterList();
            if (nameAsDeclaration == null || primaryConstructorParameterList == null) {
                return klass.getTextRange();
            } else {
                return new TextRange(
                        nameAsDeclaration.getTextRange().getStartOffset(),
                        primaryConstructorParameterList.getTextRange().getEndOffset()
                );
            }
        } else {
            // safe way
            return jetDeclaration.getTextRange();
        }
    }

    @NotNull
    public Diagnostic on(@NotNull JetDeclaration declaration, @NotNull FunctionDescriptor functionDescriptor,
            @NotNull String functionContainer)
    {
        TextRange rangeToMark = rangeToMark(declaration);

        String message = messageFormat.format(new Object[]{
                functionContainer,
                DescriptorRenderer.TEXT.render(functionDescriptor)});
        return new GenericDiagnostic(this, severity, message, declaration.getContainingFile(), rangeToMark);
    }

} //~
