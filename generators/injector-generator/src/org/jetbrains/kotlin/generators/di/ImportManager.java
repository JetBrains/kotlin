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

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.utils.Printer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class ImportManager {

    private final Map<String, Class<?>> classes = Maps.newLinkedHashMap();

    public boolean addClass(@NotNull Class<?> classToImport) {
        String simpleName = classToImport.getSimpleName();

        Class<?> imported = classes.get(simpleName);
        if (imported != null) return classToImport.equals(imported);

        classes.put(simpleName, classToImport);
        return true;
    }

    @NotNull
    public Collection<Class<?>> getImportedClasses() {
        return classes.values();
    }

    @NotNull
    public CharSequence render(@NotNull DiType type) {
        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);
        if (addClass(type.getClazz())) {
            p.print(type.getClazz().getSimpleName());
        }
        else {
            p.print(type.getClazz().getCanonicalName());
        }

        if (!type.getTypeParameters().isEmpty()) {
            p.print("<");
            for (Iterator<DiType> iterator = type.getTypeParameters().iterator(); iterator.hasNext(); ) {
                p.print(render(iterator.next()));
                if (iterator.hasNext()) {
                    p.print(", ");
                }
            }
            p.print(">");
        }
        return out;
    }
}
