class Foo<out T>(val key: T)
class Bar()

class KFlowable<T> {
    fun <R> scan(initial: R, accumulator: (R, T) -> R) {}
}

fun test(k: KFlowable<Foo<Enum<*>>>) {
    k.scan(Bar()) { s, t -> Bar() }
}

fun box(): String {
    test(KFlowable())
    return "OK"
}