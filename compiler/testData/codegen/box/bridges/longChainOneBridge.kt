// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    open fun foo(t: T) = "A"
}

open class B<T> : A<T>()

open class C : B<String>() {
    override fun foo(t: String) = "C"
}

open class D : C()

class Z : D() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val z = Z()
    val d: D = z
    val c: C = z
    val b: B<String> = z
    val a: A<String> = z
    return when {
        z.foo("") != "Z" -> "Fail #1"
        d.foo("") != "Z" -> "Fail #2"
        c.foo("") != "Z" -> "Fail #3"
        b.foo("") != "Z" -> "Fail #4"
        a.foo("") != "Z" -> "Fail #5"
        else -> "OK"
    }
}
