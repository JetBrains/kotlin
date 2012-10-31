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

package org.jetbrains.jet.lang.resolve.java.wrapper;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.TypeSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PropertyPsiData {
    public static Map<String, PropertyPsiData> collectGroupingValuesFromAccessors(List<PropertyPsiDataElement> propertyAccessors) {
        Map<String, PropertyPsiData> map = new HashMap<String, PropertyPsiData>();
        for (PropertyPsiDataElement propertyAccessor : propertyAccessors) {
            String key = propertyKeyForGrouping(propertyAccessor);

            PropertyPsiData value = map.get(key);
            if (value == null) {
                value = new PropertyPsiData();
                value.isExtension = propertyAccessor.getReceiverType() != null;
                map.put(key, value);
            }

            if (value.isExtension && (propertyAccessor.getReceiverType() == null)) {
                throw new IllegalStateException("internal error, incorrect key");
            }

            if (propertyAccessor.isGetter()) {
                if (value.getter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.getter = propertyAccessor;
            }
            else if (propertyAccessor.isSetter()) {
                if (value.setter != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.setter = propertyAccessor;
            }
            else if (propertyAccessor.isField()) {
                if (value.field != null) {
                    throw new IllegalStateException("oops, duplicate key");
                }
                value.field = propertyAccessor;
            }
            else {
                throw new IllegalStateException();
            }
        }

        return map;
    }

    private static String propertyKeyForGrouping(PropertyPsiDataElement propertyAccessor) {
        String type = key(propertyAccessor.getType());
        String receiverType = key(propertyAccessor.getReceiverType());
        return Pair.create(type, receiverType).toString();
    }

    private static String key(@Nullable TypeSource typeSource) {
        if (typeSource == null) {
            return "";
        }
        else if (typeSource.getTypeString().length() > 0) {
            return typeSource.getTypeString();
        }
        else {
            return typeSource.getPsiType().getPresentableText();
        }
    }

    @Nullable
    private PropertyPsiDataElement getter = null;
    @Nullable
    private PropertyPsiDataElement setter = null;
    @Nullable
    private PropertyPsiDataElement field = null;
    boolean isExtension = false;

    @Nullable
    public PropertyPsiDataElement getGetter() {
        return getter;
    }

    @Nullable
    public PropertyPsiDataElement getSetter() {
        return setter;
    }

    @Nullable
    public PropertyPsiDataElement getField() {
        return field;
    }

    public boolean isExtension() {
        return isExtension;
    }

    @NotNull
    public PropertyPsiDataElement getCharacteristicMember() {
        if (getter != null) {
            return getter;
        }
        if (field != null) {
            return field;
        }
        if (setter != null) {
            return setter;
        }
        throw new IllegalStateException();
    }

    public boolean isVar() {
        if (getter == null && setter == null) {
            return !field.getMember().isFinal();
        }
        return setter != null;
    }
}
