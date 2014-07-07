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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractLazyTypeParameterDescriptor extends AbstractTypeParameterDescriptor {
    public AbstractLazyTypeParameterDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source
    ) {
        super(storageManager, containingDeclaration, Annotations.EMPTY /* TODO */, name, variance, isReified, index, source);
    }

    @NotNull
    @Override
    protected TypeConstructor createTypeConstructor() {
        return new TypeConstructor() {
            @NotNull
            @Override
            public Collection<JetType> getSupertypes() {
                return AbstractLazyTypeParameterDescriptor.this.getUpperBounds();
            }

            @NotNull
            @Override
            public List<TypeParameterDescriptor> getParameters() {
                return Collections.emptyList();
            }

            @Override
            public boolean isFinal() {
                return false;
            }

            @Override
            public boolean isDenotable() {
                return true;
            }

            @Override
            public ClassifierDescriptor getDeclarationDescriptor() {
                return AbstractLazyTypeParameterDescriptor.this;
            }

            @NotNull
            @Override
            public Annotations getAnnotations() {
                return AbstractLazyTypeParameterDescriptor.this.getAnnotations();
            }

            @Override
            public String toString() {
                return getName().toString();
            }
        };
    }
}
