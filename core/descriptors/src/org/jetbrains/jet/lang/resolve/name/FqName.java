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

import java.util.List;

public final class FqName extends FqNameBase {

    @NotNull
    public static FqName fromSegments(@NotNull List<String> names) {
        String fqName = StringUtil.join(names, ".");
        return new FqName(fqName);
    }

    public static final FqName ROOT = new FqName("");

    @NotNull
    private final FqNameUnsafe fqName;

    // cache
    private transient FqName parent;

    public FqName(@NotNull String fqName) {
        this.fqName = new FqNameUnsafe(fqName, this);

        validateFqName();
    }

    public FqName(@NotNull FqNameUnsafe fqName) {
        this.fqName = fqName;

        validateFqName();
    }

    private FqName(@NotNull FqNameUnsafe fqName, FqName parent) {
        this.fqName = fqName;
        this.parent = parent;

        validateFqName();
    }


    private void validateFqName() {
        if (!isValidAfterUnsafeCheck(fqName.asString())) {
            throw new IllegalArgumentException("incorrect fq name: " + fqName);
        }
    }

    /*package*/ static boolean isValidAfterUnsafeCheck(@NotNull String qualifiedName) {
        // TODO: There's a valid name with escape char ``
        return qualifiedName.indexOf('<') < 0;
    }

    @Override
    @NotNull
    public String asString() {
        return fqName.asString();
    }

    @NotNull
    public FqNameUnsafe toUnsafe() {
        return fqName;
    }

    public boolean isRoot() {
        return fqName.isRoot();
    }

    @NotNull
    public FqName parent() {
        if (parent != null) {
            return parent;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        parent = new FqName(fqName.parent());

        return parent;
    }

    @NotNull
    public FqName child(@NotNull Name name) {
        return new FqName(fqName.child(name), this);
    }

    @NotNull
    public Name shortName() {
        return fqName.shortName();
    }

    @NotNull
    public Name shortNameOrSpecial() {
        return fqName.shortNameOrSpecial();
    }

    @NotNull
    public List<FqName> path() {
        final List<FqName> path = Lists.newArrayList();
        path.add(ROOT);
        fqName.walk(new FqNameUnsafe.WalkCallback() {
            @Override
            public void segment(@NotNull Name shortName, @NotNull FqNameUnsafe fqName) {
                // TODO: do not validate
                path.add(new FqName(fqName));
            }
        });
        return path;
    }

    @Override
    @NotNull
    public List<Name> pathSegments() {
        return fqName.pathSegments();
    }

    public boolean firstSegmentIs(@NotNull Name segment) {
        return fqName.firstSegmentIs(segment);
    }

    public boolean lastSegmentIs(@NotNull Name segment) {
        return fqName.lastSegmentIs(segment);
    }

    public boolean isAncestorOf(@NotNull FqName other) {
        String thisString = this.asString();
        String otherString = other.asString();
        return otherString.equals(thisString) || otherString.startsWith(thisString + ".");
    }

    @NotNull
    public static FqName topLevel(@NotNull Name shortName) {
        return new FqName(FqNameUnsafe.topLevel(shortName));
    }


    @Override
    public String toString() {
        return fqName.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FqName)) return false;

        FqName otherFqName = (FqName) o;

        if (!fqName.equals(otherFqName.fqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fqName.hashCode();
    }
}
