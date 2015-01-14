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
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

public abstract class Visibility {
    private final boolean isPublicAPI;
    private final String name;

    protected Visibility(@NotNull String name, boolean isPublicAPI) {
        this.isPublicAPI = isPublicAPI;
        this.name = name;
    }

    public boolean isPublicAPI() {
        return isPublicAPI;
    }

    /**
    * @return null if the answer is unknown
    */
    protected Integer compareTo(@NotNull Visibility visibility) {
        return Visibilities.compareLocal(this, visibility);
    }

    @Override
    public String toString() {
        return name;
    }

    @NotNull
    public Visibility normalize() {
        return this;
    }

    protected abstract boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from);
}
