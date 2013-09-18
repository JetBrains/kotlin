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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

public class TypeProjection {
    private final Variance projection;
    private final JetType type;

    public TypeProjection(@NotNull Variance projection, @NotNull JetType type) {
        this.projection = projection;
        this.type = type;
    }

    public TypeProjection(JetType type) {
        this(Variance.INVARIANT, type);
    }

    @NotNull
    public Variance getProjectionKind() {
        return projection;
    }

    @NotNull
    public JetType getType() {
        return type;
    }

    @Override
    public String toString() {
        if (projection == Variance.INVARIANT) {
            return type.toString();
        }
        return projection + " " + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeProjection that = (TypeProjection) o;

        if (projection != that.projection) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = projection != null ? projection.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
