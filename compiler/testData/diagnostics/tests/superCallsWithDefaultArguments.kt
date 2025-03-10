// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75578
// WITH_STDLIB

interface I {
    fun foo(p1: Int, p2: Int = 0): String
}

open class A : I {
    override fun foo(p1: Int, p2: Int): String = "A::foo $p1 $p2"
}

class B : A() {
    override fun foo(p1: Int, p2: Int): String = "B::foo $p1 $p2"

    fun bar() = listOf(
        super.foo(1, 2),
        super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>foo<!>(1),
        foo(1, 2),
        foo(1),
    )
}

fun box(): String {
    val expected = listOf(
        "A::foo 1 2",
        "A::foo 1 0",
        "B::foo 1 2",
        "B::foo 1 0",
    )
    val actual = B().bar()

    return when {
        B().bar() != expected -> "expected: $expected, but was: $actual"
        else -> "OK"
    }
}
