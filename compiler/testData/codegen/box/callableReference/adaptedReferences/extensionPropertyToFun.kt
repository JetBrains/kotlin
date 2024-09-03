val Int.foo: String
    get() = ""

val <T> T.bar: T
    get() = 1 as T

inline fun <T, R> foo(a: (T) -> R){}
inline fun <T, R> bar(a: T.() -> R){}

fun box(): String {
    foo(Int::foo)
    foo(Int::bar)
    bar(Int::foo)
    bar(Int::bar)
    return "OK"
}