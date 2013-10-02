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
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.WrappedValues;

public abstract class NullableLazyValueImpl<T> implements NullableLazyValue<T> {
    private enum State {
        NOT_COMPUTED,
        COMPUTING,
        RECURSION_WAS_DETECTED
    }

    @Nullable
    private Object value = State.NOT_COMPUTED;

    public boolean isComputed() {
        return value != State.NOT_COMPUTED;
    }

    @Override
    public T compute() {
        if (!(value instanceof State)) {
            // Computed already
            return WrappedValues.unescapeThrowable(value);
        }

        if (value == State.COMPUTING) {
            Object result = recursionDetected(true);
            if (result != State.NOT_COMPUTED) {
                value = State.RECURSION_WAS_DETECTED;
                return WrappedValues.unescapeThrowable(result);
            }
        }
        else if (value == State.RECURSION_WAS_DETECTED) {
            Object result = recursionDetected(false);
            if (result != State.NOT_COMPUTED) {
                return WrappedValues.unescapeThrowable(result);
            }
        }

        value = State.COMPUTING;
        try {
            T typedValue = doCompute();
            value = typedValue;
            postCompute(typedValue);
            return typedValue;
        }
        catch (Throwable e) {
            value = WrappedValues.escapeThrowable(e);
            throw ExceptionUtils.rethrow(e);
        }
    }

    /**
     * @return {@code NOT_COMPUTED} to proceed, a value or wrapped exception otherwise, see WrappedValues
     * @throws DO NOT throw exceptions from implementations of this method, instead return WrappedValues.escapeThrowable(exception)
     */
    public Object recursionDetected(boolean firstTime) {
        return WrappedValues.escapeThrowable(new ReenteringLazyValueComputationException());
    }

    protected abstract T doCompute();

    protected void postCompute(T value) {
        // Doing something in post-compute helps prevent infinite recursion
    }
}
