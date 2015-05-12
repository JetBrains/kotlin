package test

public final class UnboundWildcard {
    public final fun foo(): MyClass<*>? = throw UnsupportedOperationException()

    public interface MyClass<T: CharSequence?>
}
