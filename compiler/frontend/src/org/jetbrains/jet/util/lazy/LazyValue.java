package org.jetbrains.jet.util.lazy;

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
