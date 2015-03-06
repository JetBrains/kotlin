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
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;

import static org.jetbrains.kotlin.codegen.AsmUtil.CAPTURED_THIS_FIELD;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.CLOSURE;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.canHaveOuter;

public class ClassContext extends FieldOwnerContext<ClassDescriptor> {
    private final JetTypeMapper typeMapper;

    public ClassContext(
            @NotNull JetTypeMapper typeMapper,
            @NotNull ClassDescriptor contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable LocalLookup localLookup
    ) {
        super(contextDescriptor, contextKind, parentContext, typeMapper.getBindingContext().get(CLOSURE, contextDescriptor),
              contextDescriptor, localLookup);

        this.typeMapper = typeMapper;
    }

    @Override
    @Nullable
    protected StackValue.Field computeOuterExpression() {
        ClassDescriptor enclosingClass = getEnclosingClass();
        if (enclosingClass == null) return null;

        if (!canHaveOuter(typeMapper.getBindingContext(), getContextDescriptor())) return null;

        return StackValue.field(
                typeMapper.mapType(enclosingClass),
                typeMapper.mapType(getContextDescriptor()),
                CAPTURED_THIS_FIELD,
                /* isStatic = */ false,
                StackValue.LOCAL_0
        );
    }

    @Nullable
    public CodegenContext getDefaultObjectContext() {
        if (getContextDescriptor().getDefaultObjectDescriptor() != null) {
            return findChildContext(getContextDescriptor().getDefaultObjectDescriptor());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Class: " + getContextDescriptor();
    }
}
