// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +NewInference
// WITH_RUNTIME

object A {
    var result = "not ok"
}

fun test1() {
    run {
        (A) {
            A.result = "OK"
        }
    }
}

object B

operator fun A.invoke(x: () -> Unit) {
    x()
}

operator fun <K, V> Pair<K, V>.invoke(f: (x: K, y: V) -> Boolean): Boolean = f(this.first, this.second)
inline fun <reified T> Any.isType(): Boolean = this is T

fun test2(): Boolean {
    return (A to B) { k, v -> k.isType<A>() && v.isType<B>() }
}

fun box(): String {
    test1()
    if (A.result != "OK") return "fail1: ${A.result}"

    if (!test2()) return "fail2"

    return "OK"
}
