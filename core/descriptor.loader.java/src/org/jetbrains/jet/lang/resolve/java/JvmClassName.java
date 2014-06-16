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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JvmClassName {
    @NotNull
    public static JvmClassName byInternalName(@NotNull String internalName) {
        return new JvmClassName(internalName);
    }

    /**
     * WARNING: fq name cannot be uniquely mapped to JVM class name.
     */
    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        JvmClassName r = new JvmClassName(fqName.asString().replace('.', '/'));
        r.fqName = fqName;
        return r;
    }

    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull String fqName) {
        return byFqNameWithoutInnerClasses(new FqName(fqName));
    }

    private final static String CLASS_OBJECT_REPLACE_GUARD = "<class_object>";
    private final static String TRAIT_IMPL_REPLACE_GUARD = "<trait_impl>";

    // Internal name:  kotlin/Map$Entry
    // FqName:         kotlin.Map.Entry

    private final String internalName;
    private FqName fqName;

    private JvmClassName(@NotNull String internalName) {
        this.internalName = internalName;
    }

    /**
     * WARNING: internal name cannot be converted to FQ name for a class which contains dollars in the name
     */
    @NotNull
    public FqName getFqNameForClassNameWithoutDollars() {
        if (fqName == null) {
            String fqName = internalName
                    .replace(JvmAbi.CLASS_OBJECT_CLASS_NAME, CLASS_OBJECT_REPLACE_GUARD)
                    .replace(JvmAbi.TRAIT_IMPL_CLASS_NAME, TRAIT_IMPL_REPLACE_GUARD)
                    .replace('$', '.')
                    .replace('/', '.')
                    .replace(TRAIT_IMPL_REPLACE_GUARD, JvmAbi.TRAIT_IMPL_CLASS_NAME)
                    .replace(CLASS_OBJECT_REPLACE_GUARD, JvmAbi.CLASS_OBJECT_CLASS_NAME);
            this.fqName = new FqName(fqName);
        }
        return fqName;
    }

    @NotNull
    public String getInternalName() {
        return internalName;
    }

    @NotNull
    public FqName getPackageFqName() {
        int packageNameEnd = internalName.lastIndexOf("/");
        if (packageNameEnd == -1) {
            return FqName.ROOT;
        }
        return FqName.fromSegments(Arrays.asList(internalName.substring(0, packageNameEnd).split("/")));
    }

    @NotNull
    public FqName getHeuristicClassFqName() {
        String name = internalName.substring(internalName.lastIndexOf("/") + 1);
        char[] chars = name.toCharArray();
        //treat all 'stand-alone' dollars as dots, except for last and first char of class name
        for (int i = 1; i < chars.length - 1; ++i) {
            if (name.charAt(i) == '$' && name.charAt(i - 1) != '$' && name.charAt(i + 1) != '$') {
                chars[i] = '.';
            }
        }
        return new FqName(new String(chars));
    }

    @Override
    public String toString() {
        return internalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return internalName.equals(((JvmClassName) o).internalName);
    }

    @Override
    public int hashCode() {
        return internalName.hashCode();
    }
}
