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

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class FqNameBase {

    protected FqNameBase() {
        if (!(this instanceof FqName || this instanceof FqNameUnsafe)) {
            throw new AssertionError("do not use this class directly");
        }
    }

    @NotNull
    protected abstract String asString();

    @NotNull
    private FqNameUnsafe toFqNameUnsafe() {
        if (this instanceof FqName) {
            return ((FqName) this).toUnsafe();
        }
        else if (this instanceof FqNameUnsafe) {
            return ((FqNameUnsafe) this);
        }
        else {
            throw new AssertionError();
        }
    }

    public final boolean equalsTo(@NotNull FqName that) {
        return equalsTo(that.toUnsafe());
    }

    public final boolean equalsTo(@NotNull FqNameUnsafe that) {
        return toFqNameUnsafe().equals(that);
    }

    @NotNull
    public abstract List<Name> pathSegments();
}
