// IGNORE_BACKEND: JVM_IR
inline fun <T> bar() { }

inline fun <U, reified V> baz() {}

class Foo {
    inline fun <T> bar() { }

    inline fun <U, reified V> baz() {}
}
