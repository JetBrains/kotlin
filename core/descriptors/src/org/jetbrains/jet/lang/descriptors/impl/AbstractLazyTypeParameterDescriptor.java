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

import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.NotNullLazyValue;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.utils.RecursionIntolerantLazyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractLazyTypeParameterDescriptor implements TypeParameterDescriptor {

    private final Variance variance;
    private final boolean reified;
    private final int index;
    private final DeclarationDescriptor containingDeclaration;
    private final Name name;

    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<JetType> defaultType;
    private final NotNullLazyValue<Set<JetType>> upperBounds;
    private final NotNullLazyValue<JetType> upperBoundsAsType;

    public AbstractLazyTypeParameterDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index
    ) {
        this.variance = variance;
        this.containingDeclaration = containingDeclaration;
        this.index = index;
        this.name = name;
        this.reified = isReified;

        this.typeConstructor = storageManager.createLazyValue(new Computable<TypeConstructor>() {
            @Override
            public TypeConstructor compute() {
                return createTypeConstructor();
            }
        });
        this.defaultType = storageManager.createLazyValue(new Computable<JetType>() {
            @Override
            public JetType compute() {
                return createDefaultType();
            }
        });
        this.upperBounds = storageManager.createLazyValue(new Computable<Set<JetType>>() {
            @Override
            public Set<JetType> compute() {
                return resolveUpperBounds();
            }
        });
        this.upperBoundsAsType = storageManager.createLazyValue(new Computable<JetType>() {
            @Override
            public JetType compute() {
                return computeUpperBoundsAsType();
            }
        });
    }

    @Override
    public boolean isReified() {
        return reified;
    }

    @NotNull
    @Override
    public Variance getVariance() {
        return variance;
    }

    @NotNull
    @Override
    public Set<JetType> getUpperBounds() {
        return upperBounds.compute();
    }

    @NotNull
    protected abstract Set<JetType> resolveUpperBounds();

    @NotNull
    @Override
    public JetType getUpperBoundsAsType() {
        return upperBoundsAsType.compute();
    }

    @NotNull
    private JetType computeUpperBoundsAsType() {
        Set<JetType> upperBounds = getUpperBounds();
        assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();
        JetType upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
        if (upperBoundsAsType == null) {
            upperBoundsAsType = KotlinBuiltIns.getInstance().getNothingType();
        }
        return upperBoundsAsType;
    }


    @NotNull
    @Override
    public Set<JetType> getLowerBounds() {
        return Collections.singleton(getLowerBoundsAsType());
    }

    @NotNull
    @Override
    public JetType getLowerBoundsAsType() {
        return KotlinBuiltIns.getInstance().getNothingType();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor.compute();
    }

    @NotNull
    private TypeConstructor createTypeConstructor() {
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
            public boolean isSealed() {
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

            @Override
            public List<AnnotationDescriptor> getAnnotations() {
                return AbstractLazyTypeParameterDescriptor.this.getAnnotations();
            }

            @Override
            public String toString() {
                return getName().toString();
            }
        };
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return defaultType.compute();
    }

    @NotNull
    private JetType createDefaultType() {
        return new JetTypeImpl(getTypeConstructor(), new LazyScopeAdapter(new RecursionIntolerantLazyValue<JetScope>() {
                        @Override
                        protected JetScope compute() {
                            return getUpperBoundsAsType().getMemberScope();
                        }
                    }));
    }

    @Override
    public JetType getClassObjectType() {
        return null;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    @Deprecated
    public TypeParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException("Don't call substitute() on type parameters");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitTypeParameterDescriptor(this, null);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.emptyList(); // TODO
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String toString() {
        try {
            return DescriptorRenderer.DEBUG_TEXT.render(this);
        }
        catch (Exception e) {
            return this.getClass().getName() + "@" + System.identityHashCode(this);
        }
    }
}
