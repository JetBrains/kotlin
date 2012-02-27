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
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.List;

/**
 * @author abreslav
 */
public interface RedeclarationDiagnostic extends Diagnostic<PsiElement> {
    class SimpleRedeclarationDiagnostic extends AbstractDiagnostic<PsiElement> implements RedeclarationDiagnostic {

        public SimpleRedeclarationDiagnostic(@NotNull PsiElement psiElement, @NotNull String name, RedeclarationDiagnosticFactory factory) {
            super(psiElement, factory, factory.severity, factory.makeMessage(name));
        }

        @NotNull
        @Override
        public List<TextRange> getTextRanges() {
            return POSITION_REDECLARATION.mark(getPsiElement());
        }
    }

    class RedeclarationDiagnosticWithDeferredResolution implements RedeclarationDiagnostic {

        private final DeclarationDescriptor duplicatingDescriptor;
        private final BindingContext contextToResolveToDeclaration;
        private final RedeclarationDiagnosticFactory factory;
        private PsiElement element;

        public RedeclarationDiagnosticWithDeferredResolution(@NotNull DeclarationDescriptor duplicatingDescriptor, @NotNull BindingContext contextToResolveToDeclaration, RedeclarationDiagnosticFactory factory) {
            this.duplicatingDescriptor = duplicatingDescriptor;
            this.contextToResolveToDeclaration = contextToResolveToDeclaration;
            this.factory = factory;
        }

        private PsiElement resolve() {
            if (element == null) {
                element = contextToResolveToDeclaration.get(BindingContext.DESCRIPTOR_TO_DECLARATION, duplicatingDescriptor);
                assert element != null : "No element for descriptor: " + duplicatingDescriptor;
            }
            return element;
        }

        @NotNull
        @Override
        public PsiElement getPsiElement() {
            return resolve();
        }

        @NotNull
        @Override
        public List<TextRange> getTextRanges() {
            return POSITION_REDECLARATION.mark(getPsiElement());
        }

        @NotNull
        @Override
        public PsiFile getPsiFile() {
            return resolve().getContainingFile();
        }

        @NotNull
        @Override
        public AbstractDiagnosticFactory getFactory() {
            return factory;
        }

        @NotNull
        @Override
        public String getMessage() {
            return factory.makeMessage(duplicatingDescriptor.getName());
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            return factory.severity;
        }
    }

    PositioningStrategy<PsiElement> POSITION_REDECLARATION = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement element) {
            if (element instanceof JetNamedDeclaration) {
                PsiElement nameIdentifier = ((JetNamedDeclaration) element).getNameIdentifier();
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier);
                }
            }
            else if (element instanceof JetFile) {
                JetFile file = (JetFile) element;
                PsiElement nameIdentifier = file.getNamespaceHeader().getNameIdentifier();
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier);
                }
            }
            return markElement(element);
        }
    };
}
