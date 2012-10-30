/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;

/*package*/ class NoReceiverParameter implements ReceiverParameterDescriptor {
    /*package*/ static final ReceiverParameterDescriptor INSTANCE = new NoReceiverParameter();

    private NoReceiverParameter() {}

    @NotNull
    @Override
    public JetType getType() {
        throw new UnsupportedOperationException("NO_RECEIVER_PARAMETER.getType()");
    }

    @NotNull
    @Override
    public ReceiverDescriptor getValue() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        throw new UnsupportedOperationException("NO_RECEIVER_PARAMETER.getContainingDeclaration()");
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor substitute(TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitReceiverParameterDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitReceiverParameterDescriptor(this, null);
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public String toString() {
        return "NO_RECEIVER_PARAMETER";
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Name getName() {
        throw new UnsupportedOperationException("NO_RECEIVER_PARAMETER.getName()");
    }
}
