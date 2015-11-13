package test;

public final class StarProjection {
    public final MyClass<?> foo() {
        throw new UnsupportedOperationException();
    }

    public interface MyClass<T extends CharSequence> {
    }
}
