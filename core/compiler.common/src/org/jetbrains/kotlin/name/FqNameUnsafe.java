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

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Like {@link FqName} but allows '<' and '>' characters in name.
 */
public final class FqNameUnsafe {
    private static final Name ROOT_NAME = Name.special("<root>");

    private static final Function1<String, Name> STRING_TO_NAME = name -> Name.guessByFirstCharacter(name);

    @NotNull
    private final String fqName;

    // cache
    private transient FqName safe;
    private transient FqNameUnsafe parent;
    private transient Name shortName;

    FqNameUnsafe(@NotNull String fqName, @NotNull FqName safe) {
        this.fqName = fqName;
        this.safe = safe;
    }

    public FqNameUnsafe(@NotNull String fqName) {
        this.fqName = fqName;
    }

    private FqNameUnsafe(@NotNull String fqName, FqNameUnsafe parent, Name shortName) {
        this.fqName = fqName;
        this.parent = parent;
        this.shortName = shortName;
    }

    public static boolean isValid(@Nullable String qualifiedName) {
        // TODO: There's a valid name with escape char ``
        return qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0;
    }

    private void compute() {
        int lastDot = fqName.lastIndexOf('.');
        if (lastDot >= 0) {
            shortName = Name.guessByFirstCharacter(fqName.substring(lastDot + 1));
            parent = new FqNameUnsafe(fqName.substring(0, lastDot));
        }
        else {
            shortName = Name.guessByFirstCharacter(fqName);
            parent = FqName.ROOT.toUnsafe();
        }
    }

    @NotNull
    public String asString() {
        return fqName;
    }

    public boolean isSafe() {
        return safe != null || asString().indexOf('<') < 0;
    }

    @NotNull
    public FqName toSafe() {
        if (safe != null) {
            return safe;
        }
        safe = new FqName(this);
        return safe;
    }

    public boolean isRoot() {
        return fqName.isEmpty();
    }

    @NotNull
    public FqNameUnsafe parent() {
        if (parent != null) {
            return parent;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        compute();

        return parent;
    }

    @NotNull
    public FqNameUnsafe child(@NotNull Name name) {
        String childFqName;
        if (isRoot()) {
            childFqName = name.asString();
        }
        else {
            childFqName = fqName + "." + name.asString();
        }
        return new FqNameUnsafe(childFqName, this, name);
    }

    @NotNull
    public Name shortName() {
        if (shortName != null) {
            return shortName;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        compute();

        return shortName;
    }

    @NotNull
    public Name shortNameOrSpecial() {
        if (isRoot()) {
            return ROOT_NAME;
        }
        else {
            return shortName();
        }
    }

    @NotNull
    public List<Name> pathSegments() {
        return isRoot() ? new ArrayList<>() : CollectionsKt.map(pathStringSegments(), STRING_TO_NAME);
    }

    public ArrayList<String> pathStringSegments() {
        if (fqName.isEmpty()) return new ArrayList<>();
        ArrayList<String> res = new ArrayList<>(7);
        int pos = 0;
        int nextDot;
        while (true) {
            nextDot = fqName.indexOf('.', pos);
            if (nextDot < 0) break;
            res.add(fqName.substring(pos, nextDot));
            pos = nextDot + 1;
        }
        res.add(fqName.substring(pos));
        return res;
    }

    public boolean startsWith(@NotNull Name segment) {
        if (isRoot())
            return false;

        int firstDot = fqName.indexOf('.');
        String segmentAsString = segment.asString();
        return fqName.regionMatches(0, segmentAsString, 0, firstDot == -1 ? Math.max(fqName.length(), segmentAsString.length()) : firstDot);
    }

    @NotNull
    public static FqNameUnsafe topLevel(@NotNull Name shortName) {
        return new FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName);
    }

    @Override
    @NotNull
    public String toString() {
        return isRoot() ? ROOT_NAME.asString() : fqName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FqNameUnsafe)) return false;

        FqNameUnsafe that = (FqNameUnsafe) o;

        return fqName.equals(that.fqName);
    }

    @Override
    public int hashCode() {
        return fqName.hashCode();
    }
}
