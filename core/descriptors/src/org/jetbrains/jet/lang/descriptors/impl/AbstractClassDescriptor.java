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

import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.List;
import java.util.Map;

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
    public JetScope getMemberScope(List<? extends TypeProjection> typeArguments) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Illegal number of type arguments: expected "
                                                                                     + getTypeConstructor().getParameters().size() + " but was " + typeArguments.size()
                                                                                     + " for " + getTypeConstructor() + " " + getTypeConstructor().getParameters();
        if (typeArguments.isEmpty()) return getScopeForMemberLookup();

        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = SubstitutionUtils.buildSubstitutionContext(typeParameters, typeArguments);

        // Unsafe substitutor is OK, because no recursion can hurt us upon a trivial substitution:
        // all the types are written explicitly in the code already, they can not get infinite.
        // One exception is *-projections, but they need to be handled separately anyways.
        TypeSubstitutor substitutor = TypeSubstitutor.createUnsafe(substitutionContext);
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
