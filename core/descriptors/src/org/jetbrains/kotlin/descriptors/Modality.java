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

package org.jetbrains.kotlin.descriptors;

import org.jetbrains.annotations.NotNull;

public enum Modality {
    // THE ORDER OF ENTRIES MATTERS HERE
    FINAL(false),
    OPEN(true),
    ABSTRACT(true);

    private final boolean overridable;

    private Modality(boolean overridable) {
        this.overridable = overridable;
    }

    public boolean isOverridable() {
        return overridable;
    }

    @NotNull
    public static Modality convertFromFlags(boolean _abstract, boolean open) {
        if (_abstract) return ABSTRACT;
        if (open) return OPEN;
        return FINAL;
    }
}
