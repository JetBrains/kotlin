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
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.org.objectweb.asm.Type;

public final class EnclosedValueDescriptor {
    private final String fieldName;
    private final DeclarationDescriptor descriptor;
    private final StackValue innerValue;
    private final Type type;

    public EnclosedValueDescriptor(
            @NotNull String fieldName,
            @Nullable DeclarationDescriptor descriptor,
            @Nullable StackValue innerValue,
            @NotNull Type type
    ) {
        this.fieldName = fieldName;
        this.descriptor = descriptor;
        this.innerValue = innerValue;
        this.type = type;
    }

    @NotNull
    public String getFieldName() {
        return fieldName;
    }

    @Nullable
    public DeclarationDescriptor getDescriptor() {
        return descriptor;
    }

    @Nullable
    public StackValue getInnerValue() {
        return innerValue;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
    public StackValue getOuterValue(@NotNull ExpressionCodegen codegen) {
        for (LocalLookup.LocalLookupCase aCase : LocalLookup.LocalLookupCase.values()) {
            if (aCase.isCase(descriptor)) {
                return aCase.outerValue(this, codegen);
            }
        }

        throw new IllegalStateException("Can't get outer value in " + codegen + " for " + this);
    }

    @Override
    public String toString() {
        return fieldName + " " + type + " -> " + descriptor;
    }
}
