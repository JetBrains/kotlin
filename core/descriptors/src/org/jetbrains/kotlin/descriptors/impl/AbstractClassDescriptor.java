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

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass;

public abstract class AbstractClassDescriptor implements ClassDescriptor {
    private final Name name;
    protected final NotNullLazyValue<JetType> defaultType;
    private final NotNullLazyValue<JetScope> unsubstitutedInnerClassesScope;
    private final NotNullLazyValue<ReceiverParameterDescriptor> thisAsReceiverParameter;

    public AbstractClassDescriptor(@NotNull StorageManager storageManager, @NotNull Name name) {
        this.name = name;
        this.defaultType = storageManager.createLazyValue(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                return TypeUtils.makeUnsubstitutedType(AbstractClassDescriptor.this, getScopeForMemberLookup());
            }
        });
        this.unsubstitutedInnerClassesScope = storageManager.createLazyValue(new Function0<JetScope>() {
            @Override
            public JetScope invoke() {
                return new InnerClassesScopeWrapper(getScopeForMemberLookup());
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
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    protected abstract JetScope getScopeForMemberLookup();

    @Nullable
    @Override
    public JetType getClassObjectType() {
        ClassDescriptor classObject = getClassObjectDescriptor();
        return classObject == null ? null : classObject.getDefaultType();
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return unsubstitutedInnerClassesScope.invoke();
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return thisAsReceiverParameter.invoke();
    }

    @NotNull
    @Override
    public JetScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Illegal number of type arguments: expected "
                                                                                     + getTypeConstructor().getParameters().size() + " but was " + typeArguments.size()
                                                                                     + " for " + getTypeConstructor() + " " + getTypeConstructor().getParameters();
        if (typeArguments.isEmpty()) return getScopeForMemberLookup();

        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeSubstitutor.buildSubstitutionContext(typeParameters, typeArguments);

        TypeSubstitutor substitutor = TypeSubstitutor.create(substitutionContext);
        return new SubstitutingScope(getScopeForMemberLookup(), substitutor);
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
    public JetType getDefaultType() {
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
