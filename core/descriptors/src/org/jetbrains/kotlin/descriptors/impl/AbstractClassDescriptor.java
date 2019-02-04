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
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.List;

public abstract class AbstractClassDescriptor implements ClassDescriptor {
    private final Name name;
    protected final NotNullLazyValue<SimpleType> defaultType;
    private final NotNullLazyValue<MemberScope> unsubstitutedInnerClassesScope;
    private final NotNullLazyValue<ReceiverParameterDescriptor> thisAsReceiverParameter;

    public AbstractClassDescriptor(@NotNull StorageManager storageManager, @NotNull Name name) {
        this.name = name;
        this.defaultType = storageManager.createLazyValue(new Function0<SimpleType>() {
            @Override
            public SimpleType invoke() {
                return TypeUtils.makeUnsubstitutedType(
                        AbstractClassDescriptor.this, getUnsubstitutedMemberScope(),
                        new Function1<ModuleDescriptor, MemberScope>() {
                            @Override
                            public MemberScope invoke(ModuleDescriptor moduleDescriptor) {
                                ClassDescriptor descriptor = KotlinTypeKt.refineDescriptor(AbstractClassDescriptor.this, moduleDescriptor);
                                if (descriptor == null) return getUnsubstitutedMemberScope(moduleDescriptor);
                                return descriptor.getUnsubstitutedMemberScope(moduleDescriptor);
                            }
                        }
                );
            }
        });
        this.unsubstitutedInnerClassesScope = storageManager.createLazyValue(new Function0<MemberScope>() {
            @Override
            public MemberScope invoke() {
                return new InnerClassesScopeWrapper(getUnsubstitutedMemberScope());
            }
        });
        this.thisAsReceiverParameter = storageManager.createLazyValue(new Function0<ReceiverParameterDescriptor>() {
            @Override
            public ReceiverParameterDescriptor invoke() {
                return new LazyClassReceiverParameterDescriptor(AbstractClassDescriptor.this);
            }
        });
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @NotNull
    @Override
    public ClassDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedInnerClassesScope() {
        return unsubstitutedInnerClassesScope.invoke();
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return thisAsReceiverParameter.invoke();
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments, @NotNull ModuleDescriptor moduleDescriptor) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Illegal number of type arguments: expected "
                                                                                     + getTypeConstructor().getParameters().size() + " but was " + typeArguments.size()
                                                                                     + " for " + getTypeConstructor() + " " + getTypeConstructor().getParameters();
        if (typeArguments.isEmpty()) return getUnsubstitutedMemberScope(moduleDescriptor);

        TypeSubstitutor substitutor = TypeConstructorSubstitution.create(getTypeConstructor(), typeArguments).buildSubstitutor();
        return new SubstitutingScope(getUnsubstitutedMemberScope(moduleDescriptor), substitutor);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution, @NotNull ModuleDescriptor moduleDescriptor) {
        if (typeSubstitution.isEmpty()) return getUnsubstitutedMemberScope(moduleDescriptor);

        TypeSubstitutor substitutor = TypeSubstitutor.create(typeSubstitution);
        return new SubstitutingScope(getUnsubstitutedMemberScope(moduleDescriptor), substitutor);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        return getMemberScope(typeArguments, DescriptorUtils.getContainingModule(this));
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
        return getMemberScope(typeSubstitution, DescriptorUtils.getContainingModule(this));
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope() {
        return getUnsubstitutedMemberScope(DescriptorUtils.getContainingModule(this));
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) {
            return this;
        }
        return new LazySubstitutingClassDescriptor(this, substitutor);
    }

    @NotNull
    @Override
    public SimpleType getDefaultType() {
        return defaultType.invoke();
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitClassDescriptor(this, null);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }
}
