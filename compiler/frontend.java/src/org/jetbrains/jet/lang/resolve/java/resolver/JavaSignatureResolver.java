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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.TypeUsage;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class JavaSignatureResolver {
    @NotNull
    private JavaTypeTransformer typeTransformer;

    @Inject
    public void setTypeTransformer(@NotNull JavaTypeTransformer javaTypeTransformer) {
        this.typeTransformer = javaTypeTransformer;
    }

    public static class TypeParameterDescriptorInitialization {
        @NotNull
        private final TypeParameterDescriptorImpl descriptor;
        private final JavaTypeParameter javaTypeParameter;

        private TypeParameterDescriptorInitialization(
                @NotNull TypeParameterDescriptorImpl descriptor,
                @NotNull JavaTypeParameter javaTypeParameter
        ) {
            this.descriptor = descriptor;
            this.javaTypeParameter = javaTypeParameter;
        }

        @NotNull
        public TypeParameterDescriptorImpl getDescriptor() {
            return descriptor;
        }
    }

    @NotNull
    public static List<TypeParameterDescriptorInitialization> makeUninitializedTypeParameters(
            @NotNull DeclarationDescriptor container,
            @NotNull Collection<JavaTypeParameter> typeParameters
    ) {
        List<TypeParameterDescriptorInitialization> result = Lists.newArrayList();
        for (JavaTypeParameter typeParameter : typeParameters) {
            result.add(makeUninitializedTypeParameter(container, typeParameter));
        }
        return result;
    }

    @NotNull
    private static TypeParameterDescriptorInitialization makeUninitializedTypeParameter(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JavaTypeParameter typeParameter
    ) {
        TypeParameterDescriptorImpl typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDeclaration,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false,
                Variance.INVARIANT,
                typeParameter.getName(),
                typeParameter.getIndex()
        );
        return new TypeParameterDescriptorInitialization(typeParameterDescriptor, typeParameter);
    }

    private void initializeTypeParameter(
            @NotNull TypeParameterDescriptorInitialization initialization,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        TypeParameterDescriptorImpl typeParameterDescriptor = initialization.descriptor;
        Collection<JavaClassifierType> upperBounds = initialization.javaTypeParameter.getUpperBounds();
        if (upperBounds.isEmpty()) {
            typeParameterDescriptor.addUpperBound(KotlinBuiltIns.getInstance().getNullableAnyType());
        }
        else {
            for (JavaClassifierType upperBound : upperBounds) {
                JetType transformedType = typeTransformer.transformToType(upperBound, TypeUsage.UPPER_BOUND, typeVariableResolver);
                typeParameterDescriptor.addUpperBound(transformedType);
            }
        }
        typeParameterDescriptor.setInitialized();
    }

    public void initializeTypeParameters(
            @NotNull List<TypeParameterDescriptorInitialization> typeParametersInitialization,
            @NotNull DeclarationDescriptor typeParametersOwner
    ) {
        List<TypeParameterDescriptor> prevTypeParameters = Lists.newArrayList();

        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        for (TypeParameterDescriptorInitialization typeParameterDescriptor : typeParametersInitialization) {
            typeParameters.add(typeParameterDescriptor.descriptor);
        }

        for (TypeParameterDescriptorInitialization psiTypeParameter : typeParametersInitialization) {
            prevTypeParameters.add(psiTypeParameter.descriptor);

            initializeTypeParameter(psiTypeParameter, new TypeVariableResolver(typeParameters, typeParametersOwner));
        }
    }

    @NotNull
    public List<TypeParameterDescriptor> resolveTypeParameters(
            @NotNull DeclarationDescriptor owner,
            @NotNull Collection<JavaTypeParameter> typeParameters
    ) {
        List<TypeParameterDescriptorInitialization> initializations = makeUninitializedTypeParameters(owner, typeParameters);

        initializeTypeParameters(initializations, owner);

        List<TypeParameterDescriptor> result = Lists.newArrayListWithCapacity(initializations.size());

        for (TypeParameterDescriptorInitialization initialization : initializations) {
            result.add(initialization.descriptor);
        }

        return result;
    }
}
