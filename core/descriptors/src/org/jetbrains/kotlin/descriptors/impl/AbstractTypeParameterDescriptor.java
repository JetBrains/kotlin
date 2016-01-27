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

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public abstract class AbstractTypeParameterDescriptor extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    public static final List<KotlinType> FALLBACK_UPPER_BOUNDS_ON_RECURSION =
            Collections.singletonList(ErrorUtils.createErrorType("Recursion while calculating upper bounds"));

    private final Variance variance;
    private final boolean reified;
    private final int index;

    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<KotlinType> defaultType;
    private final NotNullLazyValue<List<KotlinType>> upperBounds;

    protected AbstractTypeParameterDescriptor(
            @NotNull final StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull final Name name,
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
        this.defaultType = storageManager.createLazyValue(new Function0<KotlinType>() {
            @Override
            public KotlinType invoke() {
                return KotlinTypeImpl.create(
                        Annotations.Companion.getEMPTY(),
                        getTypeConstructor(), false, Collections.<TypeProjection>emptyList(),
                        new LazyScopeAdapter(storageManager.createLazyValue(
                                new Function0<MemberScope>() {
                                    @Override
                                    public MemberScope invoke() {
                                        return TypeIntersectionScope.create("Scope for type parameter " + name.asString(), getUpperBounds());
                                    }
                                }
                        ))
                );
            }
        });
        this.upperBounds = storageManager.createLazyValueWithPostCompute(
                new Function0<List<KotlinType>>() {
                    @Override
                    public List<KotlinType> invoke() {
                        return resolveUpperBounds();
                    }
                },
                new Function1<Boolean, List<KotlinType>>() {
                    @Override
                    public List<KotlinType> invoke(Boolean aBoolean) {
                        return FALLBACK_UPPER_BOUNDS_ON_RECURSION;
                    }
                },
                new Function1<List<KotlinType>, Unit>() {
                    @Override
                    public Unit invoke(List<KotlinType> types) {
                        getSupertypeLoopChecker().findLoopsInSupertypesAndDisconnect(
                                getTypeConstructor(),
                                types,
                                new Function1<TypeConstructor, Iterable<? extends KotlinType>>() {
                                    @Override
                                    public Iterable<? extends KotlinType> invoke(TypeConstructor typeConstructor) {
                                        if (typeConstructor.getDeclarationDescriptor() instanceof AbstractTypeParameterDescriptor) {
                                            return ((AbstractTypeParameterDescriptor) typeConstructor.getDeclarationDescriptor())
                                                    .resolveUpperBounds();
                                        }
                                        return typeConstructor.getSupertypes();
                                    }
                                },
                                new Function1<KotlinType, Unit>() {
                                    @Override
                                    public Unit invoke(KotlinType type) {
                                        reportCycleError(type);
                                        return Unit.INSTANCE;
                                    }
                                }
                        );

                        if (types.isEmpty()) {
                            types.add(ErrorUtils.createErrorType("Cyclic upper bounds"));
                        }

                        return null;
                    }
                });
    }

    @NotNull
    protected abstract SupertypeLoopChecker getSupertypeLoopChecker();
    protected abstract void reportCycleError(@NotNull KotlinType type);

    @NotNull
    protected abstract List<KotlinType> resolveUpperBounds();

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

    @Override
    public boolean isCapturedFromOuterDeclaration() {
        return false;
    }

    @NotNull
    @Override
    public List<KotlinType> getUpperBounds() {
        return upperBounds.invoke();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor.invoke();
    }

    @NotNull
    @Override
    public KotlinType getDefaultType() {
        return defaultType.invoke();
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
