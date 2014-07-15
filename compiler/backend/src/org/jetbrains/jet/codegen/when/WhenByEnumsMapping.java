/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.when;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;

import java.util.HashMap;
import java.util.Map;

public class WhenByEnumsMapping {
    private static final String MAPPING_ARRAY_FIELD_PREFIX = "$EnumSwitchMapping$";
    private static final String MAPPINGS_CLASS_NAME_POSTFIX = "$WhenMappings";

    private final Map<EnumValue, Integer> map = new HashMap<EnumValue, Integer>();
    private final String enumClassInternalName;
    private final String outerClassInternalNameForExpression;
    private final String mappingsClassInternalName;
    private final int fieldNumber;

    public WhenByEnumsMapping(String enumClassInternalName, String outerClassInternalNameForExpression, int fieldNumber) {
        this.enumClassInternalName = enumClassInternalName;
        this.outerClassInternalNameForExpression = outerClassInternalNameForExpression;
        this.mappingsClassInternalName = outerClassInternalNameForExpression + MAPPINGS_CLASS_NAME_POSTFIX;
        this.fieldNumber = fieldNumber;
    }

    public int getIndexByEntry(@NotNull EnumValue value) {
        Integer result = map.get(value);
        assert result != null : "entry " + value + " has no mapping";
        return result;
    }

    public void putFirstTime(EnumValue value, int index) {
        if (!map.containsKey(value)) {
            map.put(value, index);
        }
    }

    public int size() {
        return map.size();
    }

    public String getFieldName() {
        return MAPPING_ARRAY_FIELD_PREFIX + fieldNumber;
    }

    public String getEnumClassInternalName() {
        return enumClassInternalName;
    }

    public String getOuterClassInternalNameForExpression() {
        return outerClassInternalNameForExpression;
    }

    public String getMappingsClassInternalName() {
        return mappingsClassInternalName;
    }

    public Iterable<Map.Entry<EnumValue, Integer>> enumValuesToIntMapping() {
        return map.entrySet();
    }
}
