// IGNORE_BACKEND: JS_IR, JS_IR_ES6

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun interface FunInterface {
    fun invoke()
}

private fun id(f: FunInterface): Any = f

fun box(): String {
    fun local1() {}
    fun local2() {}

    checkEqual(id(::local1), id(::local1))
    checkNotEqual(id(::local1), id(::local2))

    fun String.localExt() {}

    checkEqual(id("A"::localExt), id("A"::localExt))
    checkNotEqual(id("A"::localExt), id("B"::localExt))

    fun adapted(default: String? = "", vararg va: Int): Int = 0

    checkEqual(id(::adapted), id(::adapted))

    return "OK"
}
