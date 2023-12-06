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

import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Like {@link FqName} but allows '<' and '>' characters in name.
 */
public final class FqNameUnsafe {
    private static final Name ROOT_NAME = Name.special("<root>");
    private static final Pattern SPLIT_BY_DOTS = Pattern.compile("\\.");

    private static final Function1<String, Name> STRING_TO_NAME = new Function1<String, Name>() {
        @Override
        public Name invoke(String name) {
            return Name.guessByFirstCharacter(name);
        }
    };

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
        return isRoot() ? Collections.<Name>emptyList() : ArraysKt.map(SPLIT_BY_DOTS.split(fqName), STRING_TO_NAME);
    }

    public boolean startsWith(@NotNull Name segment) {
        if (isRoot())
            return false;

        int firstDot = fqName.indexOf('.');
        int fqNameFirstSegmentLength = firstDot == -1 ? fqName.length() : firstDot;
        String segmentAsString = segment.asString();
        return fqNameFirstSegmentLength == segmentAsString.length() &&
                fqName.regionMatches(0, segmentAsString, 0, fqNameFirstSegmentLength);
    }

    public boolean startsWith(@NotNull FqNameUnsafe other) {
        if (isRoot()) return false;

        int thisLength = fqName.length();
        int otherLength = other.fqName.length();
        if (thisLength < otherLength) return false;

        return (thisLength == otherLength || fqName.charAt(otherLength) == '.') &&
               fqName.regionMatches(0, other.fqName, 0, otherLength);
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

        if (!fqName.equals(that.fqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fqName.hashCode();
    }
}
