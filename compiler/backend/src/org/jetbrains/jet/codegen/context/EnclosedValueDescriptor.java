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

import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public final class EnclosedValueDescriptor {
    private final DeclarationDescriptor descriptor;
    private final StackValue innerValue;
    private final Type type;

    public EnclosedValueDescriptor(DeclarationDescriptor descriptor, StackValue innerValue, Type type) {
        this.descriptor = descriptor;
        this.innerValue = innerValue;
        this.type = type;
    }

    public DeclarationDescriptor getDescriptor() {
        return descriptor;
    }

    public StackValue getInnerValue() {
        return innerValue;
    }

    public Type getType() {
        return type;
    }

    public StackValue getOuterValue(ExpressionCodegen expressionCodegen) {
        GenerationState state = expressionCodegen.getState();
        for (LocalLookup.LocalLookupCase aCase : LocalLookup.LocalLookupCase.values()) {
            if (aCase.isCase(descriptor, state)) {
                return aCase.outerValue(this, expressionCodegen);
            }
        }

        throw new IllegalStateException();
    }
}
