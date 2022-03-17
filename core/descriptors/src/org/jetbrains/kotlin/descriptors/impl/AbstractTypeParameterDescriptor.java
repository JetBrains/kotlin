/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTypeParameterDescriptor extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    private final Variance variance;
    private final boolean reified;
    private final int index;

    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<SimpleType> defaultType;
    private final StorageManager storageManager;

    protected AbstractTypeParameterDescriptor(
            @NotNull final StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull final Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source,
            @NotNull final SupertypeLoopChecker supertypeLoopChecker
    ) {
        super(containingDeclaration, annotations, name, source);
        this.variance = variance;
        this.reified = isReified;
        this.index = index;

        this.typeConstructor = storageManager.createLazyValue(new Function0<TypeConstructor>() {
            @Override
            public TypeConstructor invoke() {
                return new TypeParameterTypeConstructor(storageManager, supertypeLoopChecker);
            }
        });
        this.defaultType = storageManager.createLazyValue(new Function0<SimpleType>() {
            @Override
            public SimpleType invoke() {
                return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                        TypeAttributes.Companion.getEmpty(),
                        getTypeConstructor(), Collections.<TypeProjection>emptyList(), false,
                        new LazyScopeAdapter(
                                new Function0<MemberScope>() {
                                    @Override
                                    public MemberScope invoke() {
                                        return TypeIntersectionScope.create("Scope for type parameter " + name.asString(), getUpperBounds());
                                    }
                                }
                        )
                );
            }
        });
        this.storageManager = storageManager;
    }

    protected abstract void reportSupertypeLoopError(@NotNull KotlinType type);

    @NotNull
    protected abstract List<KotlinType> resolveUpperBounds();

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
        return ((TypeParameterTypeConstructor) getTypeConstructor()).getSupertypes();
    }

    @NotNull
    @Override
    public final TypeConstructor getTypeConstructor() {
        return typeConstructor.invoke();
    }

    @NotNull
    @Override
    public SimpleType getDefaultType() {
        return defaultType.invoke();
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getOriginal() {
        return (TypeParameterDescriptor) super.getOriginal();
    }

    @NotNull
    protected List<KotlinType> processBoundsWithoutCycles(@NotNull List<KotlinType> bounds) {
        return bounds;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @NotNull
    @Override
    public StorageManager getStorageManager() {
        return storageManager;
    }

    private class TypeParameterTypeConstructor extends AbstractTypeConstructor {

        private final SupertypeLoopChecker supertypeLoopChecker;

        public TypeParameterTypeConstructor(@NotNull StorageManager storageManager, SupertypeLoopChecker supertypeLoopChecker) {
            super(storageManager);
            this.supertypeLoopChecker = supertypeLoopChecker;
        }

        @NotNull
        @Override
        protected Collection<KotlinType> computeSupertypes() {
            return resolveUpperBounds();
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

        @NotNull
        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return AbstractTypeParameterDescriptor.this;
        }

        @NotNull
        @Override
        public KotlinBuiltIns getBuiltIns() {
            return DescriptorUtilsKt.getBuiltIns(AbstractTypeParameterDescriptor.this);
        }

        @Override
        public String toString() {
            return getName().toString();
        }

        @NotNull
        @Override
        protected SupertypeLoopChecker getSupertypeLoopChecker() {
            return supertypeLoopChecker;
        }

        @Override
        protected void reportSupertypeLoopError(@NotNull KotlinType type) {
            AbstractTypeParameterDescriptor.this.reportSupertypeLoopError(type);
        }

        @NotNull
        @Override
        protected List<KotlinType> processSupertypesWithoutCycles(@NotNull List<KotlinType> supertypes) {
            return processBoundsWithoutCycles(supertypes);
        }

        @Nullable
        @Override
        protected KotlinType defaultSupertypeIfEmpty() {
            return ErrorUtils.createErrorType(ErrorTypeKind.CYCLIC_UPPER_BOUNDS);
        }

        @Override
        protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
            return classifier instanceof TypeParameterDescriptor &&
                   DescriptorEquivalenceForOverrides.INSTANCE.areTypeParametersEquivalent(
                           AbstractTypeParameterDescriptor.this,
                           (TypeParameterDescriptor) classifier,
                           true
                   );
        }
    }
}
