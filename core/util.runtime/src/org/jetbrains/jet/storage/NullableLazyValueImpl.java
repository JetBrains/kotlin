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

package org.jetbrains.jet.storage;

import org.jetbrains.annotations.Nullable;

public abstract class NullableLazyValueImpl<T> implements NullableLazyValue<T> {
    private static final Object NOT_COMPUTED = new Object();

    @Nullable
    private Object value = NOT_COMPUTED;

    @Override
    public T compute() {
        Object _value = value;
        if (_value != NOT_COMPUTED) return (T) _value;

        T typedValue = doCompute();
        value = typedValue;

        postCompute(typedValue);

        return typedValue;
    }

    protected abstract T doCompute();

    protected void postCompute(T value) {
        // Doing something in post-compute helps prevent infinite recursion
    }
}
