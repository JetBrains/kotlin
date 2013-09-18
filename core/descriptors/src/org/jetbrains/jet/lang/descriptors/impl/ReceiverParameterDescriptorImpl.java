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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;

public class ReceiverParameterDescriptorImpl extends AbstractReceiverParameterDescriptor {
    private final DeclarationDescriptor containingDeclaration;
    private final JetType type;
    private final ReceiverValue value;

    public ReceiverParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetType type,
            @NotNull ReceiverValue value
    ) {
        this.containingDeclaration = containingDeclaration;
        this.type = type;
        this.value = value;
    }

    @NotNull
    @Override
    public JetType getType() {
        return type;
    }

    @NotNull
    @Override
    public ReceiverValue getValue() {
        return value;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }
}
