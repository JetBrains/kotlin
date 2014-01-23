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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameterListOwner;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class JavaTypeParameterResolver {
    @NotNull
    private JavaTypeTransformer typeTransformer;

    @Inject
    public void setTypeTransformer(@NotNull JavaTypeTransformer javaTypeTransformer) {
        this.typeTransformer = javaTypeTransformer;
    }

    @NotNull
    public Initializer resolveTypeParameters(
            @NotNull DeclarationDescriptor ownerDescriptor,
            @NotNull JavaTypeParameterListOwner typeParameterListOwner
    ) {
        Initializer result = new Initializer(ownerDescriptor);
        for (JavaTypeParameter typeParameter : typeParameterListOwner.getTypeParameters()) {
            result.addTypeParameter(typeParameter);
        }
        return result;
    }

    public class Initializer {
        private final DeclarationDescriptor owner;
        private final List<TypeParameterDescriptor> descriptors = new ArrayList<TypeParameterDescriptor>();
        private final List<JavaTypeParameter> javaTypeParameters = new ArrayList<JavaTypeParameter>();

        private Initializer(@NotNull DeclarationDescriptor owner) {
            this.owner = owner;
        }

        private void addTypeParameter(@NotNull JavaTypeParameter typeParameter) {
            TypeParameterDescriptorImpl descriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                    owner,
                    Annotations.EMPTY, // TODO
                    false,
                    Variance.INVARIANT,
                    typeParameter.getName(),
                    typeParameter.getIndex()
            );

            descriptors.add(descriptor);
            javaTypeParameters.add(typeParameter);
        }

        @NotNull
        public List<TypeParameterDescriptor> getDescriptors() {
            return descriptors;
        }

        public void initialize() {
            TypeVariableResolver typeVariableResolver = new TypeVariableResolverImpl(getDescriptors(), owner);

            Iterator<TypeParameterDescriptor> descriptorIterator = descriptors.iterator();
            Iterator<JavaTypeParameter> typeParameterIterator = javaTypeParameters.iterator();

            while (descriptorIterator.hasNext()) {
                TypeParameterDescriptorImpl descriptor = (TypeParameterDescriptorImpl) descriptorIterator.next();
                Collection<JavaClassifierType> upperBounds = typeParameterIterator.next().getUpperBounds();

                if (upperBounds.isEmpty()) {
                    descriptor.addUpperBound(KotlinBuiltIns.getInstance().getDefaultBound());
                }
                else {
                    for (JavaClassifierType upperBound : upperBounds) {
                        descriptor.addUpperBound(typeTransformer.transformToType(upperBound, TypeUsage.UPPER_BOUND, typeVariableResolver));
                    }
                }
                descriptor.setInitialized();
            }
        }
    }
}
