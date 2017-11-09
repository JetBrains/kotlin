// WITH_RUNTIME

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun foo() { }

inline fun <T> bar() { }

inline fun <U, reified V> baz() {}

class Foo {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    inline fun foo() { }

    inline fun <T> bar() { }

    inline fun <U, reified V> baz() {}
}
