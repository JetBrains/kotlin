/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class AbstractDiagnosticFactory {

    private String name = null;

    public static Void writeFieldNamesToDiagnostics(@NotNull Class<?> declaringClass) {
        for (Field field : declaringClass.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    Object value = field.get(null);
                    if (value instanceof AbstractDiagnosticFactory) {
                        AbstractDiagnosticFactory factory = (AbstractDiagnosticFactory)value;
                        factory.setName(field.getName());
                    }
                }
                catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }

    public void setName(String name) {
        assert this.name == null;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
