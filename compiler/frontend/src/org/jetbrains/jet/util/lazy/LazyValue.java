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

package org.jetbrains.jet.util.lazy;

public abstract class LazyValue<T> {

    private enum State {
        NOT_COMPUTED,
        BEING_COMPUTED,
        COMPUTED,
        ERROR
    }

    private State state = State.NOT_COMPUTED;
    private T value;

    protected abstract T compute();

    protected T getValueOnErrorReentry() {
        throw new ReenteringLazyValueComputationException();
    }

    public boolean isComputed() {
        return state == State.ERROR || state == State.COMPUTED;
    }

    public final T get() {
        switch (state) {
            case NOT_COMPUTED:
                state = State.BEING_COMPUTED;
                value = compute();
                state = State.COMPUTED;
                return value;
            case BEING_COMPUTED:
                state = State.ERROR;
                throw new ReenteringLazyValueComputationException();
            case COMPUTED:
                return value;
            case ERROR:
                return getValueOnErrorReentry();
        }
        throw new IllegalStateException("Unreachable");
    }



}
