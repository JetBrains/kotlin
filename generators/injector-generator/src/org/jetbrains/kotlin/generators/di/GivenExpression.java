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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class GivenExpression implements Expression {
    private final String expression;
    private final Collection<DiType> typesToImport;


    public GivenExpression(@NotNull String expression) {
        this(expression, Collections.<DiType>emptyList());
    }

    public GivenExpression(@NotNull String expression, @NotNull DiType... typesToImport) {
        this(expression, Arrays.asList(typesToImport));
    }

    public GivenExpression(@NotNull String expression, @NotNull Class<?>... typesToImport) {
        this(expression, convertClassesToDiTypes(typesToImport));
    }

    private static Collection<DiType> convertClassesToDiTypes(Class<?>[] typesToImport) {
        Collection<DiType> types = Lists.newArrayList();
        for (Class<?> aClass : typesToImport) {
            types.add(new DiType(aClass));
        }
        return types;
    }

    public GivenExpression(@NotNull String expression, @NotNull Collection<DiType> typesToImport) {
        this.expression = expression;
        this.typesToImport = typesToImport;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "given<" + expression + ">";
    }

    @Override
    public DiType getType() {
        return null;
    }

    @NotNull
    @Override
    public String renderAsCode() {
        return expression;
    }

    @NotNull
    @Override
    public Collection<DiType> getTypesToImport() {
        return typesToImport;
    }
}
