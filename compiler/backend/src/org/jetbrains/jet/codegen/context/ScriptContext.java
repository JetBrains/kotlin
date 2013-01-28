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
import org.jetbrains.jet.codegen.binding.MutableClosure;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;

public class ScriptContext extends CodegenContext {
    @NotNull
    private final ScriptDescriptor scriptDescriptor;

    public ScriptContext(
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull ClassDescriptor contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable MutableClosure closure
    ) {
        super(contextDescriptor, contextKind, parentContext, closure, contextDescriptor, null);

        this.scriptDescriptor = scriptDescriptor;
    }

    @NotNull
    public ScriptDescriptor getScriptDescriptor() {
        return scriptDescriptor;
    }

    @Override
    public boolean isStatic() {
        return true;
    }
}
