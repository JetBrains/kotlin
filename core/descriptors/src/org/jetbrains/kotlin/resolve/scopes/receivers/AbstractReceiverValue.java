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

package org.jetbrains.kotlin.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.KotlinType;

public abstract class AbstractReceiverValue implements ReceiverValue {
    protected final KotlinType receiverType;
    private final ReceiverValue original;

    public AbstractReceiverValue(@NotNull KotlinType receiverType, @Nullable ReceiverValue original) {
        this.receiverType = receiverType;
        this.original = original != null ? original : this;
    }

    @Override
    @NotNull
    public KotlinType getType() {
        return receiverType;
    }

    @NotNull
    @Override
    public ReceiverValue getOriginal() {
        return original;
    }
}
