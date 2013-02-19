/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetTypeElement;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.java.TypeUsage.MEMBER_SIGNATURE_COVARIANT;

public abstract class ElementAlternativeSignatureData {
    private String error;
    private boolean isAnnotated;

    public final boolean hasErrors() {
        return error != null;
    }

    @NotNull
    public final String getError() {
        if (error == null) {
            throw new IllegalStateException("There are no errors");
        }
        return error;
    }

    protected final void setError(@Nullable String error) {
        this.error = error;
    }

    public boolean isAnnotated() {
        return this.isAnnotated;
    }

    protected final void checkForErrors() {
        if (!isAnnotated() || hasErrors()) {
            throw new IllegalStateException("Trying to read result while there is none");
        }
    }

    protected final void setAnnotated(boolean isAnnotated) {
        this.isAnnotated = isAnnotated;
    }

    protected static void checkForSyntaxErrors(PsiElement namedElement) {
        List<PsiErrorElement> syntaxErrors = AnalyzingUtils.getSyntaxErrorRanges(namedElement);

        if (!syntaxErrors.isEmpty()) {
            int errorOffset = syntaxErrors.get(0).getTextOffset();
            String syntaxErrorDescription = syntaxErrors.get(0).getErrorDescription();

            if (syntaxErrors.size() == 1) {
                throw new AlternativeSignatureMismatchException("Alternative signature has syntax error at %d: %s",
                                                                errorOffset, syntaxErrorDescription);
            }
            else {
                throw new AlternativeSignatureMismatchException("Alternative signature has %d syntax errors, first is at %d: %s",
                                                                syntaxErrors.size(), errorOffset, syntaxErrorDescription);
            }
        }
    }

    protected static JetType computeReturnType(
            @NotNull JetType originalType,
            @Nullable JetTypeReference altReturnTypeReference,
            @NotNull Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters) {
        if (altReturnTypeReference == null) {
            if (KotlinBuiltIns.getInstance().isUnit(originalType)) {
                return originalType;
            }
            else {
                throw new AlternativeSignatureMismatchException(
                        "Return type in alternative signature is missing, while in real signature it is '%s'",
                        DescriptorRenderer.TEXT.renderType(originalType));
            }
        }

        JetTypeElement typeElement = altReturnTypeReference.getTypeElement();
        assert (typeElement != null);

        return TypeTransformingVisitor.computeType(typeElement, originalType, originalToAltTypeParameters, MEMBER_SIGNATURE_COVARIANT);
    }
}
