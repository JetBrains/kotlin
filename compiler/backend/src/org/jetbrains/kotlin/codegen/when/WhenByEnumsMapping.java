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

package org.jetbrains.kotlin.codegen.when;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.resolve.constants.EnumValue;

import java.util.LinkedHashMap;
import java.util.Map;

public class WhenByEnumsMapping {
    public static final String MAPPING_ARRAY_FIELD_PREFIX = "$EnumSwitchMapping$";
    public static final String MAPPINGS_CLASS_NAME_POSTFIX = "$WhenMappings";

    private final Map<EnumValue, Integer> map = new LinkedHashMap<EnumValue, Integer>();
    private final ClassDescriptor enumClassDescriptor;
    private final String outerClassInternalNameForExpression;
    private final String mappingsClassInternalName;
    private final int fieldNumber;

    public WhenByEnumsMapping(
            @NotNull ClassDescriptor enumClassDescriptor,
            @NotNull String outerClassInternalNameForExpression,
            int fieldNumber
    ) {
        this.enumClassDescriptor = enumClassDescriptor;
        this.outerClassInternalNameForExpression = outerClassInternalNameForExpression;
        this.mappingsClassInternalName = outerClassInternalNameForExpression + MAPPINGS_CLASS_NAME_POSTFIX;
        this.fieldNumber = fieldNumber;
    }

    public int getIndexByEntry(@NotNull EnumValue value) {
        Integer result = map.get(value);
        assert result != null : "entry " + value + " has no mapping";
        return result;
    }

    public void putFirstTime(@NotNull EnumValue value, int index) {
        if (!map.containsKey(value)) {
            map.put(value, index);
        }
    }

    public int size() {
        return map.size();
    }

    @NotNull
    public String getFieldName() {
        return MAPPING_ARRAY_FIELD_PREFIX + fieldNumber;
    }

    @NotNull
    public ClassDescriptor getEnumClassDescriptor() {
        return enumClassDescriptor;
    }

    @NotNull
    public String getOuterClassInternalNameForExpression() {
        return outerClassInternalNameForExpression;
    }

    @NotNull
    public String getMappingsClassInternalName() {
        return mappingsClassInternalName;
    }

    @NotNull
    public Iterable<Map.Entry<EnumValue, Integer>> enumValuesToIntMapping() {
        return map.entrySet();
    }
}
