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

/**
 * @author abreslav
 */
public abstract class MutableDeclarationDescriptor implements DeclarationDescriptor {
    private String name;
    private final DeclarationDescriptor containingDeclaration;

    public MutableDeclarationDescriptor(DeclarationDescriptor containingDeclaration) {
        this.containingDeclaration = containingDeclaration;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        assert this.name == null : this.name;
        this.name = name;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }
}
