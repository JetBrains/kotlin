/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.KtScope;
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.Collections;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public abstract class AbstractTypeParameterDescriptor extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    private final Variance variance;
    private final boolean reified;
    private final int index;

    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<KtType> defaultType;
    private final NotNullLazyValue<Set<KtType>> upperBounds;
    private final NotNullLazyValue<KtType> upperBoundsAsType;

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
        this.defaultType = storageManager.createLazyValue(new Function0<KtType>() {
            @Override
            public KtType invoke() {
                return KtTypeImpl.create(Annotations.Companion.getEMPTY(), getTypeConstructor(), false, Collections.<TypeProjection>emptyList(),
                                         new LazyScopeAdapter(storageManager.createLazyValue(
                                               new Function0<KtScope>() {
                                                   @Override
                                                   public KtScope invoke() {
                                                       return getUpperBoundsAsType().getMemberScope();
                                                   }
                                               }
                                       )));
            }
        });
        this.upperBounds = storageManager.createRecursionTolerantLazyValue(new Function0<Set<KtType>>() {
            @Override
            public Set<KtType> invoke() {
                return resolveUpperBounds();
            }
        }, Collections.singleton(ErrorUtils.createErrorType("Recursion while calculating upper bounds")));
        this.upperBoundsAsType = storageManager.createLazyValue(new Function0<KtType>() {
            @Override
            public KtType invoke() {
                return computeUpperBoundsAsType();
            }
        });
    }

    @NotNull
    @ReadOnly
    protected abstract Set<KtType> resolveUpperBounds();

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
    public Set<KtType> getUpperBounds() {
        return upperBounds.invoke();
    }

    @NotNull
    @Override
    public KtType getUpperBoundsAsType() {
        return upperBoundsAsType.invoke();
    }

    @NotNull
    private KtType computeUpperBoundsAsType() {
        Set<KtType> upperBounds = getUpperBounds();
        assert !upperBounds.isEmpty() : "Upper bound list is empty in " + getName();
        KtType upperBoundsAsType = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, upperBounds);
        return upperBoundsAsType != null ? upperBoundsAsType : getBuiltIns(this).getNothingType();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor.invoke();
    }

    @NotNull
    @Override
    public KtType getDefaultType() {
        return defaultType.invoke();
    }

    @NotNull
    @Override
    public Set<KtType> getLowerBounds() {
        return Collections.singleton(getBuiltIns(this).getNothingType());
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
