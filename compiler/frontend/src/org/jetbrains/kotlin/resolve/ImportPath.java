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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.renderer.RenderingUtilsKt;

public final class ImportPath {
    private final @NotNull FqName fqName;
    private final @Nullable Name alias;
    private final boolean isAllUnder;

    public ImportPath(@NotNull FqName fqName, boolean isAllUnder) {
        this(fqName, isAllUnder, null);
    }

    public ImportPath(@NotNull FqName fqName, boolean isAllUnder, @Nullable Name alias) {
        this.fqName = fqName;
        this.isAllUnder = isAllUnder;
        this.alias = alias;
    }

    public ImportPath(@NotNull String pathStr) {
        if (pathStr.endsWith(".*")) {
            this.isAllUnder = true;
            this.fqName = new FqName(pathStr.substring(0, pathStr.length() - 2));
        }
        else {
            this.isAllUnder = false;
            this.fqName = new FqName(pathStr);
        }

        alias = null;
    }

    public String getPathStr() {
        return RenderingUtilsKt.render(fqName.toUnsafe()) + (isAllUnder ? ".*" : "");
    }

    @Override
    public String toString() {
        return getPathStr() + (alias != null ? " as " + alias.asString() : "");
    }

    @NotNull
    public FqName fqnPart() {
        return fqName;
    }

    @Nullable
    public Name getAlias() {
        return alias;
    }

    public boolean hasAlias() {
        return alias != null;
    }

    public boolean isAllUnder() {
        return isAllUnder;
    }

    @Nullable
    public Name getImportedName() {
        if (!isAllUnder()) {
            return alias != null ? alias : fqName.shortName();
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImportPath path = (ImportPath) o;

        if (isAllUnder != path.isAllUnder) return false;
        if (alias != null ? !alias.equals(path.alias) : path.alias != null) return false;
        if (!fqName.equals(path.fqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fqName.hashCode();
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (isAllUnder ? 1 : 0);
        return result;
    }
}
