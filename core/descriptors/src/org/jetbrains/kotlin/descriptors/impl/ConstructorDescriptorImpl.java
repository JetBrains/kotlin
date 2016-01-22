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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConstructorDescriptorImpl extends FunctionDescriptorImpl implements ConstructorDescriptor {

    protected final boolean isPrimary;

    private static final Name NAME = Name.special("<init>");

    protected ConstructorDescriptorImpl(
            @NotNull ClassDescriptor containingDeclaration,
            @Nullable ConstructorDescriptor original,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, original, annotations, NAME, kind, source);
        this.isPrimary = isPrimary;
    }

    @NotNull
    public static ConstructorDescriptorImpl create(
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull SourceElement source
    ) {
        return new ConstructorDescriptorImpl(containingDeclaration, null, annotations, isPrimary, Kind.DECLARATION, source);
    }

    public ConstructorDescriptorImpl initialize(
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull Visibility visibility,
            @NotNull List<TypeParameterDescriptor> typeParameterDescriptors
    ) {
        super.initialize(
                null, calculateDispatchReceiverParameter(),
                typeParameterDescriptors,
                unsubstitutedValueParameters, null,
                Modality.FINAL, visibility);
        return this;
    }

    public ConstructorDescriptorImpl initialize(
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull Visibility visibility
    ) {
        initialize(unsubstitutedValueParameters, visibility, getContainingDeclaration().getDeclaredTypeParameters());
        return this;
    }

    @Nullable
    private ReceiverParameterDescriptor calculateDispatchReceiverParameter() {
        ClassDescriptor classDescriptor = getContainingDeclaration();
        if (classDescriptor.isInner()) {
            DeclarationDescriptor classContainer = classDescriptor.getContainingDeclaration();
            if (classContainer instanceof ClassDescriptor) {
                return ((ClassDescriptor) classContainer).getThisAsReceiverParameter();
            }
        }
        return null;
    }

    @NotNull
    @Override
    public ClassDescriptor getContainingDeclaration() {
        return (ClassDescriptor) super.getContainingDeclaration();
    }

    @NotNull
    @Override
    public ConstructorDescriptor getOriginal() {
        return (ConstructorDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    public ConstructorDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        return (ConstructorDescriptor) super.substitute(originalSubstitutor);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitConstructorDescriptor(this, data);
    }

    @Override
    public boolean isPrimary() {
        return isPrimary;
    }

    @NotNull
    @Override
    public Collection<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();
    }

    @Override
    public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overriddenFunction) {
        throw new UnsupportedOperationException("Constructors cannot override anything");
    }

    @NotNull
    @Override
    protected ConstructorDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            boolean preserveSource
    ) {
        if (kind != Kind.DECLARATION && kind != Kind.SYNTHESIZED) {
            throw new IllegalStateException("Attempt at creating a constructor that is not a declaration: \n" +
                                            "copy from: " + this + "\n" +
                                            "newOwner: " + newOwner + "\n" +
                                            "kind: " + kind);
        }
        assert newName == null : "Attempt to rename constructor: " + this;
        return new ConstructorDescriptorImpl(
                (ClassDescriptor) newOwner,
                this,
                getAnnotations(),
                isPrimary,
                Kind.DECLARATION,
                getSourceToUseForCopy(preserveSource, original)
        );
    }

    @NotNull
    @Override
    public ConstructorDescriptor copy(DeclarationDescriptor newOwner, Modality modality, Visibility visibility, Kind kind, boolean copyOverrides) {
        //noinspection ConstantConditions
        return (ConstructorDescriptor) doSubstitute(
                newCopyBuilder()
                        .setOwner(newOwner)
                        .setModality(modality)
                        .setVisibility(visibility)
                        .setKind(kind)
                        .setCopyOverrides(copyOverrides)
        );
    }
}
