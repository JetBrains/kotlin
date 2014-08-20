package test

public final class UnboundWildcard {
    public final fun foo(): MyClass<*>? = throw UnsupportedOperationException()

    public trait MyClass<T: CharSequence?>
}
