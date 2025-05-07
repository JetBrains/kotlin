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

class C {
    fun target1() {}
    fun target2() {}
    
    fun adapted1(s: String? = null): String = s!!
    fun adapted2(vararg s: String): String = s[0]
}

fun box(): String {
    val c0 = C()

    checkEqual(id(c0::target1), id(c0::target1))
    checkEqual(id(c0::target1), target1FromOtherFile(c0))

    checkNotEqual(id(c0::target1), id(c0::target2))

    checkEqual(id(c0::adapted1), id(c0::adapted1))
    checkEqual(id(c0::adapted1), adapted1FromOtherFile(c0))
    checkEqual(id(c0::adapted2), id(c0::adapted2))
    checkEqual(id(c0::adapted2), adapted2FromOtherFile(c0))
    checkNotEqual(id(c0::adapted1), id(c0::adapted2))

    val c1 = C()
    checkNotEqual(id(c0::target1), id(c1::target1))
    checkNotEqual(id(c0::target1), id(c1::target2))
    checkNotEqual(id(c0::adapted1), id(c1::adapted1))

    return "OK"
}

// FILE: fromOtherFile.kt

private fun id(f: FunInterface): Any = f

fun target1FromOtherFile(c0: C): Any = id(c0::target1)
fun adapted1FromOtherFile(c0: C): Any = id(c0::adapted1)
fun adapted2FromOtherFile(c0: C): Any = id(c0::adapted2)
