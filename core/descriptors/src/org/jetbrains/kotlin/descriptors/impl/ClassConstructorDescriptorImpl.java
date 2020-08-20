/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassConstructorDescriptorImpl extends FunctionDescriptorImpl implements ClassConstructorDescriptor {

    protected final boolean isPrimary;

    protected ClassConstructorDescriptorImpl(
            @NotNull ClassDescriptor containingDeclaration,
            @Nullable ConstructorDescriptor original,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, original, annotations, SpecialNames.INIT, kind, source);
        this.isPrimary = isPrimary;
    }

    @NotNull
    public static ClassConstructorDescriptorImpl create(
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull SourceElement source
    ) {
        return new ClassConstructorDescriptorImpl(containingDeclaration, null, annotations, isPrimary, Kind.DECLARATION, source);
    }

    @NotNull
    public static ClassConstructorDescriptorImpl createSynthesized(
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull SourceElement source
    ) {
        return new ClassConstructorDescriptorImpl(containingDeclaration, null, annotations, isPrimary, Kind.SYNTHESIZED, source);
    }

    public ClassConstructorDescriptorImpl initialize(
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull DescriptorVisibility visibility,
            @NotNull List<TypeParameterDescriptor> typeParameterDescriptors
    ) {
        super.initialize(
                null, calculateDispatchReceiverParameter(), calculateAdditionalReceiverParameters(),
                typeParameterDescriptors,
                unsubstitutedValueParameters, null,
                Modality.FINAL, visibility);
        return this;
    }

    public ClassConstructorDescriptorImpl initialize(
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull DescriptorVisibility visibility
    ) {
        initialize(unsubstitutedValueParameters, visibility, getContainingDeclaration().getDeclaredTypeParameters());
        return this;
    }

    @Nullable
    public ReceiverParameterDescriptor calculateDispatchReceiverParameter() {
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
    List<ReceiverParameterDescriptor> calculateAdditionalReceiverParameters() {
        ClassDescriptor classDescriptor = getContainingDeclaration();
        if (!classDescriptor.getContextReceivers().isEmpty()) {
            return classDescriptor.getContextReceivers();
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public ClassDescriptor getContainingDeclaration() {
        return (ClassDescriptor) super.getContainingDeclaration();
    }

    @NotNull
    @Override
    public ClassDescriptor getConstructedClass() {
        return getContainingDeclaration();
    }

    @NotNull
    @Override
    public ClassConstructorDescriptor getOriginal() {
        return (ClassConstructorDescriptor) super.getOriginal();
    }

    @Nullable
    @Override
    public ClassConstructorDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        return (ClassConstructorDescriptor) super.substitute(originalSubstitutor);
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
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
        assert overriddenDescriptors.isEmpty() : "Constructors cannot override anything";
    }

    @NotNull
    @Override
    protected ClassConstructorDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        if (kind != Kind.DECLARATION && kind != Kind.SYNTHESIZED) {
            throw new IllegalStateException("Attempt at creating a constructor that is not a declaration: \n" +
                                            "copy from: " + this + "\n" +
                                            "newOwner: " + newOwner + "\n" +
                                            "kind: " + kind);
        }
        assert newName == null : "Attempt to rename constructor: " + this;
        return new ClassConstructorDescriptorImpl(
                (ClassDescriptor) newOwner,
                this,
                annotations,
                isPrimary,
                Kind.DECLARATION,
                source
        );
    }

    @NotNull
    @Override
    public ClassConstructorDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            DescriptorVisibility visibility,
            Kind kind,
            boolean copyOverrides
    ) {
        return (ClassConstructorDescriptor) super.copy(newOwner, modality, visibility, kind, copyOverrides);
    }
}
