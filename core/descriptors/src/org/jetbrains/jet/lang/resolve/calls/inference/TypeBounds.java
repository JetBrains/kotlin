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

package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collection;

public interface TypeBounds {
    @NotNull
    Variance getVarianceOfPosition();

    @NotNull
    TypeParameterDescriptor getTypeVariable();

    @NotNull
    Collection<Bound> getBounds();

    boolean isEmpty();

    @Nullable
    JetType getValue();

    @NotNull
    Collection<JetType> getValues();

    enum BoundKind {
        LOWER_BOUND, UPPER_BOUND, EXACT_BOUND
    }

    class Bound {
        public final JetType type;
        public final BoundKind kind;
        public final ConstraintPosition position;

        public Bound(@NotNull JetType type, @NotNull BoundKind kind, @NotNull ConstraintPosition position) {
            this.type = type;
            this.kind = kind;
            this.position = position;
        }
    }
}
