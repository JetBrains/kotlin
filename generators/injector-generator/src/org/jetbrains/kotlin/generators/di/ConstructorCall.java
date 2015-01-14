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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ConstructorCall implements Expression {
    private final Constructor<?> constructor;
    private final List<Field> constructorArguments = Lists.newArrayList();

    ConstructorCall(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public List<Field> getConstructorArguments() {
        return constructorArguments;
    }

    @Override
    public String toString() {
        return constructor.toString();
    }

    @NotNull
    @Override
    public String renderAsCode() {
        StringBuilder builder = new StringBuilder("new " + constructor.getDeclaringClass().getSimpleName() + "(");
        for (Iterator<Field> iterator = constructorArguments.iterator(); iterator.hasNext(); ) {
            Field argument = iterator.next();
            if (argument.isPublic()) {
                builder.append(argument.getGetterName()).append("()");
            }
            else {
                builder.append(argument.getName());
            }
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    @NotNull
    @Override
    public Collection<DiType> getTypesToImport() {
        return Collections.singletonList(new DiType(constructor.getDeclaringClass()));
    }

    @NotNull
    @Override
    public DiType getType() {
        return new DiType(constructor.getDeclaringClass());
    }
}
