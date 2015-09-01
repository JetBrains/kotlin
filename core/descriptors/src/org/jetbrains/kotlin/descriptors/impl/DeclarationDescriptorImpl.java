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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;

public abstract class DeclarationDescriptorImpl extends AnnotatedImpl implements DeclarationDescriptor {

    @NotNull
    private final Name name;

    public DeclarationDescriptorImpl(@NotNull Annotations annotations, @NotNull Name name) {
        super(annotations);
        this.name = name;
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

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }

    @Override
    public String toString() {
        return toString(this);
    }

    @NotNull
    public static String toString(@NotNull DeclarationDescriptor descriptor) {
        try {
            return DescriptorRenderer.DEBUG_TEXT.render(descriptor) +
                   "[" + descriptor.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(descriptor)) + "]";
        } catch (Throwable e) {
            // DescriptionRenderer may throw if this is not yet completely initialized
            // It is very inconvenient while debugging
            return descriptor.getClass().getSimpleName() + " " + descriptor.getName();
        }
    }
}
