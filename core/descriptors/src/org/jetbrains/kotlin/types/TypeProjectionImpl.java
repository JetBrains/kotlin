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

import org.jetbrains.annotations.NotNull;

public class TypeProjectionImpl extends TypeProjectionBase {
    private final Variance projection;
    private final JetType type;

    public TypeProjectionImpl(@NotNull Variance projection, @NotNull JetType type) {
        this.projection = projection;
        this.type = type;
    }

    public TypeProjectionImpl(@NotNull JetType type) {
        this(Variance.INVARIANT, type);
    }

    @Override
    @NotNull
    public Variance getProjectionKind() {
        return projection;
    }

    @Override
    @NotNull
    public JetType getType() {
        return type;
    }

    @Override
    public boolean isStarProjection() {
        return false;
    }
}
