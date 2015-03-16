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

package org.jetbrains.kotlin.name;

import org.jetbrains.annotations.NotNull;

/**
 * A class name which is used to uniquely identify a Kotlin class.
 *
 * If local = true, the class represented by this id is either itself local or is an inner class of some local class. This also means that
 * the first non-class container of the class is not a package.
 * In the case of a local class, relativeClassName consists of a single name including all callables' and class' names all the way up to
 * the package, separated by dollar signs. If a class is an inner of local, relativeClassName would consist of two names,
 * the second one being the class' short name.
 */
public final class ClassId {
    @NotNull
    public static ClassId topLevel(@NotNull FqName topLevelFqName) {
        return new ClassId(topLevelFqName.parent(), topLevelFqName.shortName());
    }

    private final FqName packageFqName;
    private final FqNameUnsafe relativeClassName;
    private final boolean local;

    public ClassId(@NotNull FqName packageFqName, @NotNull FqNameUnsafe relativeClassName, boolean local) {
        this.packageFqName = packageFqName;
        assert !relativeClassName.isRoot() :
                "Class name must not be root: " + packageFqName + (local ? " (local)" : "");
        this.relativeClassName = relativeClassName;
        this.local = local;
    }

    public ClassId(@NotNull FqName packageFqName, @NotNull Name topLevelName) {
        this(packageFqName, FqNameUnsafe.topLevel(topLevelName), false);
    }

    @NotNull
    public FqName getPackageFqName() {
        return packageFqName;
    }

    @NotNull
    public FqNameUnsafe getRelativeClassName() {
        return relativeClassName;
    }

    public boolean isLocal() {
        return local;
    }

    @NotNull
    public ClassId createNestedClassId(@NotNull Name name) {
        return new ClassId(getPackageFqName(), relativeClassName.child(name), local);
    }

    @NotNull
    public ClassId getOuterClassId() {
        return new ClassId(getPackageFqName(), relativeClassName.parent(), local);
    }

    public boolean isNestedClass() {
        return !relativeClassName.parent().isRoot();
    }

    @NotNull
    public FqNameUnsafe asSingleFqName() {
        if (packageFqName.isRoot()) return relativeClassName;
        return new FqNameUnsafe(packageFqName.asString() + "." + relativeClassName.asString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassId id = (ClassId) o;

        return packageFqName.equals(id.packageFqName) &&
               relativeClassName.equals(id.relativeClassName) &&
               local == id.local;
    }

    @Override
    public int hashCode() {
        int result = packageFqName.hashCode();
        result = 31 * result + relativeClassName.hashCode();
        result = 31 * result + Boolean.valueOf(local).hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (packageFqName.isRoot()) return "/" + relativeClassName;
        return packageFqName.toString().replace('.', '/') + "/" + relativeClassName;
    }

    @NotNull
    public static ClassId fromString(@NotNull String string) {
        int lastSlash = string.lastIndexOf("/");
        if (lastSlash == -1) {
            throw new IllegalArgumentException("Class id should contain slash: " + string);
        }
        FqNameUnsafe relativeClassName = new FqNameUnsafe(string.substring(lastSlash + 1));
        FqName packageFqName = new FqName(string.substring(0, lastSlash).replace('/', '.'));
        return new ClassId(packageFqName, relativeClassName, false);
    }
}
