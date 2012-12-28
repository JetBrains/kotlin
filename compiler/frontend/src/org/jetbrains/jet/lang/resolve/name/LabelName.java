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

package org.jetbrains.jet.lang.resolve.name;

import org.jetbrains.annotations.NotNull;

public class LabelName {

    @NotNull
    private final String name;

    public LabelName(@NotNull String name) {
        if (name.startsWith("@")) {
            // label may contain @, and may not contain
            //throw new IllegalArgumentException("@ must be chopped: " + name);
        }

        if (name.length() == 0) {
            // label can be empty
            //throw new IllegalStateException("label cannot be empty");
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LabelName name1 = (LabelName) o;

        if (!name.equals(name1.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
