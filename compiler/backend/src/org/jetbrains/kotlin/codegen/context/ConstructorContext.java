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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;

import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;

public class ConstructorContext extends MethodContext {
    private static final StackValue LOCAL_1 = StackValue.local(1, OBJECT_TYPE);
    private boolean thisInitialized = false;

    public ConstructorContext(
            @NotNull ConstructorDescriptor contextDescriptor,
            @NotNull OwnerKind kind,
            @NotNull CodegenContext parent,
            @Nullable MutableClosure closure
    ) {
        super(contextDescriptor, kind, parent, closure, false);
    }

    @Override
    public StackValue getOuterExpression(StackValue prefix, boolean ignoreNoOuter) {
        StackValue stackValue = closure != null && closure.getCapturedOuterClassDescriptor() != null ? LOCAL_1 : null;
        if (!ignoreNoOuter && stackValue == null) {
            throw new UnsupportedOperationException("Don't know how to generate outer expression for " + getContextDescriptor());
        }
        return stackValue;
    }

    public ConstructorDescriptor getConstructorDescriptor() {
        return (ConstructorDescriptor) getContextDescriptor();
    }

    public boolean isThisInitialized() {
        return thisInitialized;
    }

    public void setThisInitialized(boolean thisInitialized) {
        this.thisInitialized = thisInitialized;
    }

    @Override
    public boolean isContextWithUninitializedThis() {
        return !isThisInitialized();
    }

    @Override
    public String toString() {
        return "Constructor: " + (isThisInitialized() ? "" : "UNINITIALIZED ") + getContextDescriptor();
    }
}
