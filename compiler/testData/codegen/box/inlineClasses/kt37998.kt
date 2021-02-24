inline class Z(val x: Int)

class A {
    fun foo() = Z(42)
}

fun test(a: A?) = a?.foo()!!

fun box(): String {
    val t = test(A())
    if (t.x != 42) throw AssertionError("$t")
    return "OK"
}