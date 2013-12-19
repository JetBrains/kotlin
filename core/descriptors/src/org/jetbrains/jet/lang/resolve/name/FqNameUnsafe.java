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

package org.jetbrains.jet.lang.resolve.name;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Like {@link FqName} but allows '<' and '>' characters in name.
 */
public final class FqNameUnsafe extends FqNameBase {

    public static final Name ROOT_NAME = Name.special("<root>");

    @NotNull
    private final String fqName;

    // cache
    private transient FqName safe;
    private transient FqNameUnsafe parent;
    private transient Name shortName;

    FqNameUnsafe(@NotNull String fqName, @NotNull FqName safe) {
        this.fqName = fqName;
        this.safe = safe;

        validateFqName();
    }

    public FqNameUnsafe(@NotNull String fqName) {
        this.fqName = fqName;

        validateFqName();
    }

    private FqNameUnsafe(@NotNull String fqName, FqNameUnsafe parent, Name shortName) {
        this.fqName = fqName;
        this.parent = parent;
        this.shortName = shortName;

        validateFqName();
    }


    private void validateFqName() {
        if (!isValid(fqName)) {
            throw new IllegalArgumentException("incorrect fq name: " + fqName);
        }
    }

    public static boolean isValid(@Nullable String qualifiedName) {
        // TODO: There's a valid name with escape char ``
        return qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0;
    }

    private void compute() {
        int lastDot = fqName.lastIndexOf('.');
        if (lastDot >= 0) {
            shortName = Name.guess(fqName.substring(lastDot + 1));
            parent = new FqNameUnsafe(fqName.substring(0, lastDot));
        }
        else {
            shortName = Name.guess(fqName);
            parent = FqName.ROOT.toUnsafe();
        }
    }



    @NotNull
    public String asString() {
        return fqName;
    }

    public boolean isSafe() {
        if (safe != null) {
            return true;
        }
        return FqName.isValidAfterUnsafeCheck(asString());
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
        return fqName.equals("");
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

    interface WalkCallback {
        void segment(@NotNull Name shortName, @NotNull FqNameUnsafe fqName);
    }

    @NotNull
    public List<FqNameUnsafe> path() {
        final List<FqNameUnsafe> path = Lists.newArrayList();
        path.add(FqName.ROOT.toUnsafe());
        walk(new WalkCallback() {
            @Override
            public void segment(@NotNull Name shortName, @NotNull FqNameUnsafe fqName) {
                path.add(fqName);
            }
        });
        return path;
    }

    @Override
    @NotNull
    public List<Name> pathSegments() {
        final List<Name> path = Lists.newArrayList();
        walk(new WalkCallback() {
            @Override
            public void segment(@NotNull Name shortName, @NotNull FqNameUnsafe fqName) {
                path.add(shortName);
            }
        });
        return path;
    }


    void walk(@NotNull WalkCallback callback) {
        if (isRoot()) {
            return;
        }

        int pos = fqName.indexOf('.');

        if (pos < 0) {
            if (this.parent == null) {
                this.parent = FqName.ROOT.toUnsafe();
            }
            if (this.shortName == null) {
                this.shortName = Name.guess(fqName);
            }
            callback.segment(shortName, this);
            return;
        }

        Name firstSegment = Name.guess(fqName.substring(0, pos));
        FqNameUnsafe last = new FqNameUnsafe(firstSegment.asString(), FqName.ROOT.toUnsafe(), firstSegment);
        callback.segment(firstSegment, last);

        while (true) {
            int next = fqName.indexOf('.', pos + 1);
            if (next < 0) {
                if (this.parent == null) {
                    this.parent = last;
                }
                Name shortName = Name.guess(fqName.substring(pos + 1));
                if (this.shortName == null) {
                    this.shortName = shortName;
                }
                callback.segment(shortName, this);
                return;
            }

            Name shortName = Name.guess(fqName.substring(pos + 1, next));
            last = new FqNameUnsafe(fqName.substring(0, next), last, shortName);
            callback.segment(shortName, last);

            pos = next;
        }
    }

    public boolean firstSegmentIs(@NotNull Name segment) {
        if (isRoot()) {
            return false;
        }
        List<Name> pathSegments = pathSegments();
        return pathSegments.get(0).equals(segment);
    }

    public boolean lastSegmentIs(@NotNull Name segment) {
        if (isRoot()) {
            return false;
        }
        return shortName().equals(segment);
    }

    @NotNull
    public static FqNameUnsafe fromSegments(@NotNull List<Name> names) {
        String fqName = StringUtil.join(names, ".");
        return new FqNameUnsafe(fqName);
    }



    @NotNull
    public static FqNameUnsafe topLevel(@NotNull Name shortName) {
        return new FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName);
    }


    @Override
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
