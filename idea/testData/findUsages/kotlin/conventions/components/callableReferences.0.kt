// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>a: Int, val b: Int) {
    fun f() {}
}

fun f1(): A = A(1, 2)

class X {
    fun f2() = A(0, 0)
}

fun foo(x: X) {
    val fun1 = A::f

    val fun2 = ::f1
    val fun3 = x::f2
    val (a1, b1) = fun2()
    val (a2, b2) = fun3()

    val constructor = ::A
    val (a3, b3) = constructor(1, 2)

    val (a4, b4) = A::class.java.newInstance()
}