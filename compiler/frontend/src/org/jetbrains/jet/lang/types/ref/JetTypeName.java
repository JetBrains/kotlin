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

package org.jetbrains.jet.lang.types.ref;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collections;
import java.util.List;

public class JetTypeName {
    // generic types one day
    private final FqName className;
    private final List<JetTypeName> arguments;

    public JetTypeName(@NotNull FqName className, @NotNull List<JetTypeName> arguments) {
        this.className = className;
        this.arguments = arguments;
    }

    @NotNull
    public FqName getClassName() {
        return className;
    }

    @NotNull
    public List<JetTypeName> getArguments() {
        return arguments;
    }

    @NotNull
    public static JetTypeName fromJavaClass(@NotNull Class<?> clazz) {
        if (clazz.getTypeParameters().length != 0) {
            throw new IllegalArgumentException("cannot create type reference: actual type parameters unknown: " + clazz);
        }
        FqName fqName = new FqName(clazz.getName());
        return withoutParameters(fqName);
    }

    @NotNull
    public static JetTypeName withoutParameters(@NotNull FqName fqName) {
        return new JetTypeName(fqName, Collections.<JetTypeName>emptyList());
    }

    @NotNull
    public static JetTypeName parse(@NotNull String value) {
        return JetTypeNameParser.parse(value);
    }
}
