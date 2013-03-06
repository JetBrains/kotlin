package test

public final class StarProjection: Object() {
    public final fun foo(): MyClass<*> = throw UnsupportedOperationException()

    public trait MyClass<T: CharSequence?>: Object
}
