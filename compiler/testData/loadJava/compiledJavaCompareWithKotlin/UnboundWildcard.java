package test;

public final class UnboundWildcard {
    public final MyClass<?> foo() {
        throw new UnsupportedOperationException();
    }

    public interface MyClass<T extends CharSequence> {
    }
}
