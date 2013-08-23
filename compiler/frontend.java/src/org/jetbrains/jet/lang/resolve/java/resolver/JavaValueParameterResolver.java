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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class JavaValueParameterResolver {
    private JavaTypeTransformer typeTransformer;

    public JavaValueParameterResolver() {
    }

    @NotNull
    private ValueParameterDescriptor resolveParameterDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            int i,
            @NotNull JavaValueParameter parameter,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        PsiParameter psiParameter = parameter.getPsi();

        PsiType psiType = psiParameter.getType();

        // TODO: must be very slow, make it lazy?
        Name name = getParameterName(i, parameter);

        TypeUsage typeUsage = JavaTypeTransformer
                .adjustTypeUsageWithMutabilityAnnotations(psiParameter, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT);
        JetType outType = typeTransformer.transformToType(psiType, typeUsage, typeVariableResolver);

        JetType varargElementType;
        if (psiType instanceof PsiEllipsisType) {
            varargElementType = KotlinBuiltIns.getInstance().getArrayElementType(TypeUtils.makeNotNullable(outType));
            outType = TypeUtils.makeNotNullable(outType);
        }
        else {
            varargElementType = null;
        }

        JetType transformedType;
        PsiAnnotation notNullAnnotation = JavaAnnotationResolver
                .findAnnotationWithExternal(psiParameter, JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION);
        if (notNullAnnotation != null) {
            transformedType = TypeUtils.makeNullableAsSpecified(outType, false);
        }
        else {
            transformedType = outType;
        }
        return new ValueParameterDescriptorImpl(
                containingDeclaration,
                i,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                name,
                transformedType,
                false,
                varargElementType
        );
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @NotNull
    private static Name getParameterName(int number, @NotNull JavaValueParameter parameter) {
        Name psiParameterName = parameter.getName();
        return psiParameterName != null ? psiParameterName : Name.identifier("p" + number);
    }

    @NotNull
    public JavaDescriptorResolver.ValueParameterDescriptors resolveParameterDescriptors(
            @NotNull DeclarationDescriptor container,
            @NotNull Collection<JavaValueParameter> parameters,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(parameters.size());
        int index = 0;
        for (JavaValueParameter parameter : parameters) {
            ValueParameterDescriptor parameterDescriptor = resolveParameterDescriptor(container, index, parameter, typeVariableResolver);
            result.add(parameterDescriptor);
            index++;
        }
        return new JavaDescriptorResolver.ValueParameterDescriptors(null, result);
    }
}