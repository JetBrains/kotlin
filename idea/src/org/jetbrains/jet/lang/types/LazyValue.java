package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
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

    public State getState() {
        return state;
    }

    @NotNull
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
                throw new ReenteringLazyValueComputationException();
        }
        throw new IllegalStateException("Unreachable");
    }



}
