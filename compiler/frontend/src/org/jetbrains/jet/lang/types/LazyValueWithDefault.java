package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public abstract class LazyValueWithDefault<T> extends LazyValue<T> {
    private final T defaultValue;

    protected LazyValueWithDefault(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    protected T getValueOnErrorReentry() {
        return defaultValue;
    }
}
