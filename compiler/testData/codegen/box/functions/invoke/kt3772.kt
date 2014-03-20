//KT-3772 Invoke and overload resolution ambiguity

open class A {
    fun invoke(f: A.() -> Unit) = 1
}

class B {
    fun invoke(f: B.() -> Unit) = 2
}

open class C
val C.attr = A()

open class D: C()
val D.attr = B()


fun box(): String {
    val d = D()
    return if (d.attr {} == 2) "OK" else "fail"
}