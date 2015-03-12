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

package org.jetbrains.kotlin.types;

public abstract class TypeProjectionBase implements TypeProjection {

    @Override
    public String toString() {
        if (isStarProjection()) {
            return "*";
        }
        if (getProjectionKind() == Variance.INVARIANT) {
            return getType().toString();
        }
        return getProjectionKind() + " " + getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeProjection)) return false;

        TypeProjection that = (TypeProjection) o;

        if (getProjectionKind() != that.getProjectionKind()) return false;
        if (!getType().equals(that.getType())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getProjectionKind().hashCode();
        result = 31 * result + (isStarProjection() ? 17 : getType().hashCode());
        return result;
    }
}
