/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
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
            if (nameAsDeclaration == null){
                return klass.getTextRange();
            }
            PsiElement primaryConstructorParameterList = klass.getPrimaryConstructorParameterList();
            if (primaryConstructorParameterList == null) {
                return nameAsDeclaration.getTextRange();
            }
            return new TextRange(
                    nameAsDeclaration.getTextRange().getStartOffset(),
                    primaryConstructorParameterList.getTextRange().getEndOffset()
            );
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
