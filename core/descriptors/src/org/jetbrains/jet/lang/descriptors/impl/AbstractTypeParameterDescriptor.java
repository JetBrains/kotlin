/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractTypeParameterDescriptor extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    private final Variance variance;
    private final boolean reified;
    private final int index;

    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<JetType> defaultType;
    private final NotNullLazyValue<Set<JetType>> upperBounds;
    private final NotNullLazyValue<JetType> upperBoundsAsType;

    protected AbstractTypeParameterDescriptor(
            @NotNull final StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, source);
        this.variance = variance;
        this.reified = isReified;
        this.index = index;

        this.typeConstructor = storageManager.createLazyValue(new Function0<TypeConstructor>() {
            @Override
            public TypeConstructor invoke() {
                return createTypeConstructor();
            }
        });
        this.defaultType = storageManager.createLazyValue(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                return new JetTypeImpl(Annotations.EMPTY, getTypeConstructor(), false, Collections.<TypeProjection>emptyList(),
                                       new LazyScopeAdapter(storageManager.createLazyValue(
                                               new Function0<JetScope>() {
                                                   @Override
                                                   public JetScope invoke() {
                                                       return getUpperBoundsAsType().getMemberScope();
                                                   }
                                               }
                                       )));
            }
        });
        this.upperBounds = storageManager.createLazyValue(new Function0<Set<JetType>>() {
            @Override
            public Set<JetType> invoke() {
                return resolveUpperBounds();
            }
        });
        this.upperBoundsAsType = storageManager.createLazyValue(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                return computeUpperBoundsAsType();
            }
        });
    }

    @NotNull
    @ReadOnly
    protected abstract Set<JetType> resolveUpperBounds();

    @NotNull
    protected abstract TypeConstructor createTypeConstructor();

    @NotNull
    @Override
    public Variance getVariance() {
        return variance;
    }

    @Override
    public boolean isReified() {
        return reified;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    @Override
    public Set<JetType> getUpperBounds() {
        return upperBounds.invoke();
    }

    @NotNull
    @Override
    public JetType getUpperBoundsAsType() {
        return upperBoundsAsType.invoke();
    }

    @NotNull
    private JetType computeUpperBoundsAsType() {
        Set<JetType> upperBounds = getUpperBounds();
        assert !upperBounds.isEmpty() : "Upper bound list is empty in " + getName();
        JetType upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.DEFAULT, upperBounds);
        return upperBoundsAsType != null ? upperBoundsAsType : KotlinBuiltIns.getInstance().getNothingType();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor.invoke();
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return defaultType.invoke();
    }

    @Override
    public JetType getClassObjectType() {
        // TODO: class object bounds
        return null;
    }

    @NotNull
    @Override
    public Set<JetType> getLowerBounds() {
        return Collections.singleton(KotlinBuiltIns.getInstance().getNothingType());
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
}
