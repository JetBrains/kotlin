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

package org.jetbrains.kotlin.generators.di;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class GetSingleton implements Expression {

    public static GetSingleton byMethod(@NotNull Class<?> singletonClass, @NotNull String methodName) {
        return new GetSingleton(singletonClass, methodName, "()");
    }

    public static GetSingleton byField(@NotNull Class<?> singletonClass, @NotNull String fieldName) {
        return new GetSingleton(singletonClass, fieldName, "");
    }

    private final Class<?> singletonClass;
    private final String memberName;
    private final String callSuffix;

    private GetSingleton(@NotNull Class<?> singletonClass, @NotNull String name, String callSuffix) {
        this.singletonClass = singletonClass;
        this.memberName = name;
        this.callSuffix = callSuffix;
    }

    @Override
    public String toString() {
        return renderAsCode();
    }

    @NotNull
    @Override
    public String renderAsCode() {
        return singletonClass.getSimpleName() + "." + memberName + callSuffix;
    }

    @NotNull
    @Override
    public Collection<DiType> getTypesToImport() {
        return Collections.singletonList(new DiType(singletonClass));
    }

    @NotNull
    @Override
    public DiType getType() {
        return new DiType(singletonClass);
    }
}
