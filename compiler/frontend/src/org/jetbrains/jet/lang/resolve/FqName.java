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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class FqName {

    public static final FqName ROOT = new FqName("");

    @NotNull
    private final String fqName;

    // cache
    private transient FqName parent;
    private transient String shortName;


    public FqName(@NotNull String fqName) {
        this.fqName = fqName;

        validateFqName();
    }

    private FqName(@NotNull String fqName, FqName parent, String shortName) {
        this.fqName = fqName;
        this.parent = parent;
        this.shortName = shortName;

        validateFqName();
    }


    private void validateFqName() {
        if (fqName.length() == 0) {
            return;
        }

        // TODO: prohibit < everywhere
        if (fqName.indexOf('/') >= 0 || fqName.charAt(0) == '<') {
            throw new IllegalArgumentException("incorrect fq name: " + fqName);
        }
    }

    private void compute() {
        int lastDot = fqName.lastIndexOf('.');
        if (lastDot >= 0) {
            shortName = fqName.substring(lastDot + 1);
            parent = new FqName(fqName.substring(0, lastDot));
        } else {
            shortName = fqName;
            parent = ROOT;
        }
    }



    @NotNull
    public String getFqName() {
        return fqName;
    }

    public boolean isRoot() {
        return fqName.equals("");
    }
    @NotNull
    public FqName parent() {
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
    public FqName child(@NotNull String name) {
        String childFqName;
        if (isRoot()) {
            childFqName = name;
        } else {
            childFqName = fqName + "." + name;
        }
        return new FqName(childFqName, this, name);
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

    private interface WalkCallback {
        void segment(@NotNull String shortName, @NotNull FqName fqName);
    }

    @NotNull
    public List<FqName> path() {
        final List<FqName> path = Lists.newArrayList();
        path.add(ROOT);
        walk(new WalkCallback() {
            @Override
            public void segment(@NotNull String shortName, @NotNull FqName fqName) {
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
            public void segment(@NotNull String shortName, @NotNull FqName fqName) {
                path.add(shortName);
            }
        });
        return path;
    }


    private void walk(@NotNull WalkCallback callback) {
        if (isRoot()) {
            return;
        }

        int pos = fqName.indexOf('.');

        if (pos < 0) {
            if (this.parent == null) {
                this.parent = ROOT;
            }
            if (this.shortName == null) {
                this.shortName = fqName;
            }
            callback.segment(fqName, this);
            return;
        }

        String firstSegment = fqName.substring(0, pos);
        FqName last = new FqName(firstSegment, ROOT, firstSegment);
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
            last = new FqName(fqName.substring(0, next), last, shortName);
            callback.segment(shortName, last);

            pos = next;
        }
    }



    @NotNull
    public static FqName topLevel(@NotNull String shortName) {
        if (shortName.indexOf('.') >= 0) {
            throw new IllegalArgumentException();
        }
        return new FqName(shortName, ROOT, shortName);
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

        FqName that = (FqName) o;

        if (fqName != null ? !fqName.equals(that.fqName) : that.fqName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // generated by Idea
        return fqName != null ? fqName.hashCode() : 0;
    }
}
