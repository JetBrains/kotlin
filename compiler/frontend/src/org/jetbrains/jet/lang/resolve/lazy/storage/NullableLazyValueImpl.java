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

package org.jetbrains.jet.lang.resolve.lazy.storage;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.WrappedValues;

public abstract class NullableLazyValueImpl<T> implements NullableLazyValue<T> {
    @Nullable
    private Object value = null;

    @Override
    public T compute() {
        Object _value = value;
        if (_value != null) return WrappedValues.unescapeNull(_value);

        T typedValue = doCompute();
        value = WrappedValues.escapeNull(typedValue);

        postCompute(typedValue);

        return typedValue;
    }

    protected abstract T doCompute();

    protected void postCompute(T value) {
        // Doing something in post-compute helps prevent infinite recursion
    }
}
