// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    open fun <U> foo(t: T, u: U) = "A"
}

open class B : A<String>()

class Z : B() {
    override fun <U> foo(t: String, u: U) = "Z"
}


fun box(): String {
    val z = Z()
    val b: B = z
    val a: A<String> = z
    return when {
        z.foo("", 0) != "Z" -> "Fail #1"
        b.foo("", 0) != "Z" -> "Fail #2"
        a.foo("", 0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
