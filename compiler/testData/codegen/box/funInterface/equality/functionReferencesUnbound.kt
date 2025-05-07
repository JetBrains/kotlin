// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// FILE: test.kt

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

fun target1() {}
fun target2() {}

fun adapted1(s: String? = null): String = s!!
fun adapted2(vararg s: String): String = s[0]

fun box(): String {
    checkEqual(id(::target1), id(::target1))
    checkEqual(id(::target1), target1FromOtherFile())

    checkNotEqual(id(::target1), id(::target2))

    checkEqual(id(::adapted1), id(::adapted1))
    checkEqual(id(::adapted1), adapted1FromOtherFile())
    checkEqual(id(::adapted2), id(::adapted2))
    checkEqual(id(::adapted2), adapted2FromOtherFile())
    checkNotEqual(id(::adapted1), id(::adapted2))

    return "OK"
}

// FILE: fromOtherFile.kt

private fun id(f: FunInterface): Any = f

fun target1FromOtherFile(): Any = id(::target1)
fun adapted1FromOtherFile(): Any = id(::adapted1)
fun adapted2FromOtherFile(): Any = id(::adapted2)
