package org.jetbrains.jet.storage;

import org.jetbrains.annotations.NotNull;

public abstract class NotNullLazyValueWithDefault<T> extends NotNullLazyValueImpl<T> {
    private final T defaultValue;

    protected NotNullLazyValueWithDefault(@NotNull T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Object recursionDetected() {
        return defaultValue;
    }
}
