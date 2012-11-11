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

package org.jetbrains.jet.codegen.context;

import org.jetbrains.jet.codegen.OwnerKind;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.CLOSURE;

/**
 * @author alex.tkachman
*/
public class ClassContext extends CodegenContext {
    public ClassContext(
            JetTypeMapper typeMapper,
            ClassDescriptor contextDescriptor,
            OwnerKind contextKind,
            CodegenContext parentContext,
            LocalLookup localLookup
    ) {
        //noinspection SuspiciousMethodCalls
        super(contextDescriptor, contextKind, parentContext, typeMapper.getBindingContext().get(CLOSURE,
                                                                                                                 contextDescriptor),
              contextDescriptor,
              localLookup);
        initOuterExpression(typeMapper, contextDescriptor);
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public String toString() {
        return "Class: " + getContextDescriptor();
    }
}
