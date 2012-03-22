/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Like {@link FqName} but allows '<' and '>' characters in name.
 *
 * @author Stepan Koltsov
 */
public class FqNameUnsafe {

    @NotNull
    private final String fqName;

    // cache
    private transient FqName safe;
    private transient FqNameUnsafe parent;
    private transient String shortName;

    FqNameUnsafe(@NotNull String fqName, @NotNull FqName safe) {
        this.fqName = fqName;
        this.safe = safe;

        validateFqName();
    }

    public FqNameUnsafe(@NotNull String fqName) {
        this.fqName = fqName;

        validateFqName();
    }

    private FqNameUnsafe(@NotNull String fqName, FqNameUnsafe parent, String shortName) {
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
            shortName = fqName.substring(lastDot + 1);
            parent = new FqNameUnsafe(fqName.substring(0, lastDot));
        } else {
            shortName = fqName;
            parent = FqName.ROOT.toUnsafe();
        }
    }



    @NotNull
    public String getFqName() {
        return fqName;
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
    public FqNameUnsafe child(@NotNull String name) {
        String childFqName;
        if (isRoot()) {
            childFqName = name;
        } else {
            childFqName = fqName + "." + name;
        }
        return new FqNameUnsafe(childFqName, this, name);
    }

    @NotNull
    public String shortName() {
        if (shortName != null) {
            return shortName;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        compute();

        return shortName;
    }

    interface WalkCallback {
        void segment(@NotNull String shortName, @NotNull FqNameUnsafe fqName);
    }

    @NotNull
    public List<FqNameUnsafe> path() {
        final List<FqNameUnsafe> path = Lists.newArrayList();
        path.add(FqName.ROOT.toUnsafe());
        walk(new WalkCallback() {
            @Override
            public void segment(@NotNull String shortName, @NotNull FqNameUnsafe fqName) {
                path.add(fqName);
            }
        });
        return path;
    }

    @NotNull
    public List<String> pathSegments() {
        final List<String> path = Lists.newArrayList();
        walk(new WalkCallback() {
            @Override
            public void segment(@NotNull String shortName, @NotNull FqNameUnsafe fqName) {
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
                this.shortName = fqName;
            }
            callback.segment(fqName, this);
            return;
        }

        String firstSegment = fqName.substring(0, pos);
        FqNameUnsafe last = new FqNameUnsafe(firstSegment, FqName.ROOT.toUnsafe(), firstSegment);
        callback.segment(firstSegment, last);

        for (;;) {
            int next = fqName.indexOf('.', pos + 1);
            if (next < 0) {
                if (this.parent == null) {
                    this.parent = last;
                }
                String shortName = fqName.substring(pos + 1);
                if (this.shortName == null) {
                    this.shortName = shortName;
                }
                callback.segment(shortName, this);
                return;
            }

            String shortName = fqName.substring(pos + 1, next);
            last = new FqNameUnsafe(fqName.substring(0, next), last, shortName);
            callback.segment(shortName, last);

            pos = next;
        }
    }



    @NotNull
    public static FqNameUnsafe topLevel(@NotNull String shortName) {
        if (shortName.indexOf('.') >= 0) {
            throw new IllegalArgumentException();
        }
        return new FqNameUnsafe(shortName, FqName.ROOT.toUnsafe(), shortName);
    }


    @Override
    public String toString() {
        return isRoot() ? "<root>" : fqName;
    }

    @Override
    public boolean equals(Object o) {
        // generated by Idea
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FqNameUnsafe that = (FqNameUnsafe) o;

        if (fqName != null ? !fqName.equals(that.fqName) : that.fqName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // generated by Idea
        return fqName != null ? fqName.hashCode() : 0;
    }
}
