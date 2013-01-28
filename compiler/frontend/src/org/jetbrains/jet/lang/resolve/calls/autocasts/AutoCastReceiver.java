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

package org.jetbrains.jet.lang.resolve.calls.autocasts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.receivers.AbstractReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValueVisitor;
import org.jetbrains.jet.lang.types.JetType;

public class AutoCastReceiver extends AbstractReceiverValue {
    private final ReceiverValue original;
    private final boolean canCast;

    public AutoCastReceiver(@NotNull ReceiverValue original, @NotNull JetType castTo, boolean canCast) {
        super(castTo);
        this.original = original;
        this.canCast = canCast;
    }

    public boolean canCast() {
        return canCast;
    }

    @NotNull
    public ReceiverValue getOriginal() {
        return original;
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverValueVisitor<R, D> visitor, D data) {
        return original.accept(visitor, data);
    }

    @Override
    public String toString() {
        return "(" + original + " as " + getType() + ")";
    }
}
