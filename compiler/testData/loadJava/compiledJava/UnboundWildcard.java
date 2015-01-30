package test;

import java.util.Collection;

public final class UnboundWildcard {
    public final MyClass<?> foo() {
        throw new UnsupportedOperationException();
    }

    public interface MyClass<T extends CharSequence> {
    }

    public final Collection<?> collection() {
        throw new UnsupportedOperationException();
    }
}
