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
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;

import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class ConstructorContext extends MethodContext {
    private static final StackValue local1 = StackValue.local(1, OBJECT_TYPE);

    public ConstructorContext(
            ConstructorDescriptor contextDescriptor,
            OwnerKind kind,
            @NotNull CodegenContext parent
    ) {
        super(contextDescriptor, kind, parent);

        ClassDescriptor type = getEnclosingClass();
        outerExpression = type != null ? local1 : null;
    }

    @Override
    public StackValue getOuterExpression(StackValue prefix, boolean ignoreNoOuter) {
        return outerExpression;
    }

    @Override
    public String toString() {
        return "Constructor: " + getContextDescriptor().getName();
    }

    @NotNull
    @Override
    public CodegenContext getParentContext() {
        //noinspection ConstantConditions
        return super.getParentContext();
    }
}
