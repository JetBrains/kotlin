val Int.foo: String
    get() = ""

val <T> T.bar: T
    get() = 1 as T

var Int.baz: String
    get() = ""
    set(value) {}

var <T> T.qux: T
    get() = 1 as T
    set(value) { }

inline fun <T, R> foo(a: (T) -> R){}
inline fun <T, R> bar(a: T.() -> R){}

fun box(): String {
    foo(Int::foo)
    foo(Int::bar)
    foo(Int::baz)
    foo(Int::qux)
    bar(Int::foo)
    bar(Int::bar)
    bar(Int::baz)
    bar(Int::qux)
    return "OK"
}