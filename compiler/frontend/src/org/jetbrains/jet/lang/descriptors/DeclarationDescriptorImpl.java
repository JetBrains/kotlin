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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class DeclarationDescriptorImpl extends AnnotatedImpl implements Named, DeclarationDescriptor {

    private final String name;
    private final DeclarationDescriptor containingDeclaration;

    public DeclarationDescriptorImpl(@Nullable DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(annotations);
        this.name = name;
        this.containingDeclaration = containingDeclaration;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }

    @Override
    public String toString() {
        try {
            return DescriptorRenderer.TEXT.render(this) + "[" + getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + "]";
        } catch (Throwable e) {
            // DescriptionRenderer may throw if this is not yet completely initialized
            // It is very inconvenient while debugging
            return this.getClass().getName() + "@" + System.identityHashCode(this);
        }
    }
}
