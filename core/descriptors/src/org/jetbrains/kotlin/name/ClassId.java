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

public final class ClassId {
    @NotNull
    public static ClassId topLevel(@NotNull FqName topLevelFqName) {
        return new ClassId(topLevelFqName.parent(), topLevelFqName.shortName());
    }

    private final FqName packageFqName;
    private final FqNameUnsafe relativeClassName;

    public ClassId(@NotNull FqName packageFqName, @NotNull FqNameUnsafe relativeClassName) {
        this.packageFqName = packageFqName;
        assert !relativeClassName.isRoot() : "Class name must not be root. " + packageFqName;
        this.relativeClassName = relativeClassName;
    }

    public ClassId(@NotNull FqName packageFqName, @NotNull Name topLevelName) {
        this(packageFqName, FqNameUnsafe.topLevel(topLevelName));
    }

    @NotNull
    public FqName getPackageFqName() {
        return packageFqName;
    }

    @NotNull
    public FqNameUnsafe getRelativeClassName() {
        return relativeClassName;
    }

    @NotNull
    public ClassId createNestedClassId(@NotNull Name name) {
        return new ClassId(getPackageFqName(), relativeClassName.child(name));
    }

    @NotNull
    public ClassId getOuterClassId() {
        return new ClassId(getPackageFqName(), relativeClassName.parent());
    }

    public boolean isTopLevelClass() {
        return relativeClassName.parent().isRoot();
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

        if (!packageFqName.equals(id.packageFqName)) return false;
        if (!relativeClassName.equals(id.relativeClassName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = packageFqName.hashCode();
        result = 31 * result + relativeClassName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (packageFqName.isRoot()) return "/" + relativeClassName;
        return packageFqName.toString().replace('.', '/') + "/" + relativeClassName;
    }
}
