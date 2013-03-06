package test

public final class UnboundWildcard: Object() {
    public final fun foo(): MyClass<*>? = throw UnsupportedOperationException()

    public trait MyClass<T: CharSequence?>: Object
}
