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

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiFieldWrapper;
import org.jetbrains.jet.lang.types.JetType;

import java.util.HashMap;

public class AlternativeFieldSignatureData extends ElementAlternativeSignatureData {
    private final PsiFieldWrapper field;

    private JetType altReturnType;

    public AlternativeFieldSignatureData(@NotNull PsiFieldWrapper field, @NotNull JetType originalReturnType, boolean isVar) {
        String signature = field.getSignatureAnnotation().signature();
        this.field = field;

        if (signature.isEmpty()) {
            setAnnotated(false);
            return;
        }

        setAnnotated(true);
        Project project = field.getPsiMember().getProject();
        JetProperty altPropertyDeclaration = JetPsiFactory.createProperty(project, signature);

        try {
            checkForSyntaxErrors(altPropertyDeclaration);
            checkFieldAnnotation(altPropertyDeclaration, field, isVar);
            altReturnType = computeReturnType(originalReturnType, altPropertyDeclaration.getTypeRef(),
                                              new HashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>());
        }
        catch (AlternativeSignatureMismatchException e) {
            setError(e.getMessage());
        }
    }

    @NotNull
    public JetType getReturnType() {
        checkForErrors();
        return altReturnType;
    }

    private static void checkFieldAnnotation(JetProperty altProperty, PsiFieldWrapper fieldWrapper, boolean isVar) {
        if (!ComparatorUtil.equalsNullable(fieldWrapper.getName(), altProperty.getName())) {
            throw new AlternativeSignatureMismatchException("Field name mismatch, original: %s, alternative: %s",
                                                            fieldWrapper.getName(), altProperty.getName());
        }

        if (altProperty.getTypeRef() == null) {
            throw new AlternativeSignatureMismatchException("Field annotation for shouldn't have type reference");
        }

        if (altProperty.getGetter() != null || altProperty.getSetter() != null) {
            throw new AlternativeSignatureMismatchException("Field annotation for shouldn't have getters and setters");
        }

        if (altProperty.isVar() != isVar) {
            throw new AlternativeSignatureMismatchException("Wrong mutability in annotation for field");
        }

        if (altProperty.getInitializer() != null) {
            throw new AlternativeSignatureMismatchException("Default value is not expected in annotation for field");
        }
    }
}
