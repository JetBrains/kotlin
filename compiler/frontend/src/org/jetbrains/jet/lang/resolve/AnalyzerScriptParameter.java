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

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ref.JetTypeName;

public class AnalyzerScriptParameter {
    @NotNull
    private final Name name;
    @NotNull
    private final JetTypeName type;

    public AnalyzerScriptParameter(@NotNull Name name, @NotNull JetTypeName type) {
        this.name = name;
        this.type = type;
    }

    public AnalyzerScriptParameter(@NotNull String name, @NotNull String type) {
        this(Name.identifier(name), JetTypeName.parse(type));
    }

    @NotNull
    public Name getName() {
        return name;
    }

    @NotNull
    public JetTypeName getType() {
        return type;
    }
}
