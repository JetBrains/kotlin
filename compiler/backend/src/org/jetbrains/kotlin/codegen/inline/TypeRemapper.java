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
import org.jetbrains.org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;

public class TypeRemapper extends Remapper {
    //typeMapping field could be changed outside through method processing
    private final Map<String, String> typeMapping;

    private Map<String, String> additionalMappings;

    //typeMapping field could be changed outside through method processing
    private TypeRemapper(@NotNull Map<String, String> typeMapping) {
        this.typeMapping = typeMapping;
    }

    public TypeRemapper(@NotNull TypeRemapper remapper, @NotNull Map<String, String> newTypeMappings) {
        this(createNewAndMerge(remapper, newTypeMappings));
    }

    public static TypeRemapper createEmpty() {
        return new TypeRemapper(new HashMap<String, String>());
    }

    public static TypeRemapper createFrom(Map<String, String> mappings) {
        return new TypeRemapper(mappings);
    }

    @NotNull
    private static Map<String, String> createNewAndMerge(@NotNull TypeRemapper remapper, @NotNull Map<String, String> additionalTypeMappings) {
        Map<String, String> map = new HashMap<String, String>(remapper.typeMapping);
        map.putAll(additionalTypeMappings);
        return map;
    }

    public void addMapping(String type, String newType) {
        typeMapping.put(type, newType);
    }

    public boolean hasNoAdditionalMapping(String type) {
        return typeMapping.containsKey(type);
    }

    @Override
    public String map(String type) {
        String newType = typeMapping.get(type);
        if (newType != null) {
            return newType;
        }

        if (additionalMappings != null) {
            newType = additionalMappings.get(type);
            if (newType != null) {
                return newType;
            }
        }

        return type;
    }

    public void addAdditionalMappings(String oldName, String newName) {
        if (additionalMappings == null) additionalMappings = new HashMap<String, String>();
        additionalMappings.put(oldName, newName);
    }
}
