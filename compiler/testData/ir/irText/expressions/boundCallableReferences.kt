// FIR_IDENTICAL

class A {
    fun foo() {}
    val bar = 0
}

fun A.qux() {}

val test1 = A()::foo

val test2 = A()::bar

val test3 = A()::qux
