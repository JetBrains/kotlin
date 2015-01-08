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
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;

import java.util.List;

// SCRIPT: script as field owner context
public class ScriptContext extends FieldOwnerContext<ClassDescriptor> {
    private final ScriptDescriptor scriptDescriptor;
    private final List<ScriptDescriptor> earlierScripts;

    public ScriptContext(
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull List<ScriptDescriptor> earlierScripts,
            @NotNull ClassDescriptor contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable MutableClosure closure
    ) {
        super(contextDescriptor, contextKind, parentContext, closure, contextDescriptor, null);
        this.scriptDescriptor = scriptDescriptor;
        this.earlierScripts = earlierScripts;
    }

    @NotNull
    public ScriptDescriptor getScriptDescriptor() {
        return scriptDescriptor;
    }

    @NotNull
    public List<ScriptDescriptor> getEarlierScripts() {
        return earlierScripts;
    }

    @NotNull
    public String getScriptFieldName(@NotNull ScriptDescriptor scriptDescriptor) {
        int index = earlierScripts.indexOf(scriptDescriptor);
        if (index < 0) {
            throw new IllegalStateException("Unregistered script: " + scriptDescriptor);
        }
        return "script$" + (index + 1);
    }

    @Override
    public boolean isStatic() {
        return true;
    }
}
