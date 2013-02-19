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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;

public class ConstructorDescriptorImpl extends FunctionDescriptorImpl implements ConstructorDescriptor {

    private final boolean isPrimary;

    private static final Name NAME = Name.special("<init>");

    public ConstructorDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, boolean isPrimary) {
        super(containingDeclaration, annotations, NAME, Kind.DECLARATION);
        this.isPrimary = isPrimary;
    }

    public ConstructorDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull ConstructorDescriptor original, @NotNull List<AnnotationDescriptor> annotations, boolean isPrimary) {
        super(containingDeclaration, original, annotations, NAME, Kind.DECLARATION);
        this.isPrimary = isPrimary;
    }

    public ConstructorDescriptorImpl initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, Visibility visibility) {
        return initialize(typeParameters, unsubstitutedValueParameters, visibility, false);
    }

    public ConstructorDescriptorImpl initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, Visibility visibility, boolean isStatic) {
        super.initialize(null, isStatic ? NO_RECEIVER_PARAMETER : getExpectedThisObject(getContainingDeclaration()), typeParameters, unsubstitutedValueParameters, null, Modality.FINAL, visibility);
        return this;
    }

    @Nullable
    private static ReceiverParameterDescriptor getExpectedThisObject(@NotNull ClassDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        return DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration);
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
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();
    }

    @Override
    public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overriddenFunction) {
        throw new UnsupportedOperationException("Constructors cannot override anything");
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        if (kind != Kind.DECLARATION) {
            throw new IllegalStateException();
        }
        return new ConstructorDescriptorImpl(
                (ClassDescriptor) newOwner,
                this,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                isPrimary);
    }

    @NotNull
    @Override
    public ConstructorDescriptor copy(DeclarationDescriptor newOwner, Modality modality, Visibility visibility, Kind kind, boolean copyOverrides) {
        throw new UnsupportedOperationException("Constructors should not be copied for overriding");
    }
}
