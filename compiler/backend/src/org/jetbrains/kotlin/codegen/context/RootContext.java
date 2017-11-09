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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;

public class RootContext extends CodegenContext<RootContext.FakeDescriptor> {
    private final GenerationState state;

    public RootContext(@NotNull GenerationState state) {
        super(new FakeDescriptor(), OwnerKind.PACKAGE, null, null, null, null);
        this.state = state;
    }

    @Override
    @NotNull
    public GenerationState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "ROOT";
    }

    static class FakeDescriptor implements DeclarationDescriptor {
        @NotNull
        @Override
        public DeclarationDescriptor getOriginal() {
            throw new IllegalStateException();
        }

        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            throw new IllegalStateException();
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            throw new IllegalStateException();
        }

        @Override
        public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Annotations getAnnotations() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Name getName() {
            throw new IllegalStateException();
        }
    }
}
