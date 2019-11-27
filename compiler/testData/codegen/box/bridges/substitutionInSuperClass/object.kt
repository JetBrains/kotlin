// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    open fun foo(t: T) = "A"
}

open class B : A<String>()

object Z : B() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val o = object : B() {
        override fun foo(t: String) = "o"
    }
    val zb: B = Z
    val ob: B = o
    val za: A<String> = Z
    val oa: A<String> = o

    return when {
        Z.foo("")  != "Z" -> "Fail #1"
        o.foo("")  != "o" -> "Fail #2"
        zb.foo("") != "Z" -> "Fail #3"
        ob.foo("") != "o" -> "Fail #4"
        za.foo("") != "Z" -> "Fail #5"
        oa.foo("") != "o" -> "Fail #6"
        else -> "OK"
    }
}
