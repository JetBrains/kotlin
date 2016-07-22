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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NameGenerator {

    private final String generatorClass;

    private int nextLambdaIndex = 1;
    private int nextWhenIndex = 1;

    private final Map<String, NameGenerator> subGenerators = new HashMap<String, NameGenerator>();

    public NameGenerator(String generatorClass) {
        this.generatorClass = generatorClass;
    }

    public String getGeneratorClass() {
        return generatorClass;
    }

    private String genLambdaClassName() {
        return generatorClass + "$" + nextLambdaIndex++;
    }

    private String genWhenClassName(@NotNull String original) {
        return generatorClass + "$" + nextWhenIndex++ + WhenMappingTransformationInfo.TRANSFORMED_WHEN_MAPPING_MARKER + original;
    }

    public NameGenerator subGenerator(String inliningMethod) {
        NameGenerator generator = subGenerators.get(inliningMethod);
        if (generator == null) {
            generator = new NameGenerator(generatorClass + "$" + inliningMethod);
            subGenerators.put(inliningMethod, generator);
        }
        return generator;
    }

    @NotNull
    public NameGenerator subGenerator(boolean lambdaNoWhen, @Nullable String nameSuffix) {
        String generatorClass = lambdaNoWhen ? genLambdaClassName() : genWhenClassName(nameSuffix);
        assert !subGenerators.containsKey(generatorClass) : "Name generator for regenerated class should be unique: " + generatorClass;
        NameGenerator generator = new NameGenerator(generatorClass);
        subGenerators.put(generatorClass, generator);
        return generator;
    }
}
