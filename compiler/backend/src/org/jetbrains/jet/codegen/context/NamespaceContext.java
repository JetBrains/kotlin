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

package org.jetbrains.jet.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.OwnerKind;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;

public class NamespaceContext extends FieldOwnerContext<NamespaceDescriptor> {
    public NamespaceContext(@NotNull NamespaceDescriptor contextDescriptor, @Nullable CodegenContext parent, @NotNull OwnerKind kind) {
        super(contextDescriptor, kind, parent, null, null, null);
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public String toString() {
        return "Namespace: " + getContextDescriptor().getName();
    }
}
