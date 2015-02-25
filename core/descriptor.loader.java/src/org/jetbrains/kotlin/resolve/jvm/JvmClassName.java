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

package org.jetbrains.kotlin.resolve.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;

public class JvmClassName {
    @NotNull
    public static JvmClassName byInternalName(@NotNull String internalName) {
        return new JvmClassName(internalName);
    }

    @NotNull
    public static JvmClassName byClassId(@NotNull ClassId classId) {
        FqName packageFqName = classId.getPackageFqName();
        String relativeClassName = classId.getRelativeClassName().asString().replace('.', '$');
        return packageFqName.isRoot()
               ? new JvmClassName(relativeClassName)
               : new JvmClassName(packageFqName.asString().replace('.', '/') + "/" + relativeClassName);
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
                    .replace(JvmAbi.TRAIT_IMPL_CLASS_NAME, TRAIT_IMPL_REPLACE_GUARD)
                    .replace('$', '.')
                    .replace('/', '.')
                    .replace(TRAIT_IMPL_REPLACE_GUARD, JvmAbi.TRAIT_IMPL_CLASS_NAME);
            this.fqName = new FqName(fqName);
        }
        return fqName;
    }

    @NotNull
    public FqName getPackageFqName() {
        int lastSlash = internalName.lastIndexOf("/");
        if (lastSlash == -1) return FqName.ROOT;
        return new FqName(internalName.substring(0, lastSlash).replace('/', '.'));
    }

    @NotNull
    public String getInternalName() {
        return internalName;
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
