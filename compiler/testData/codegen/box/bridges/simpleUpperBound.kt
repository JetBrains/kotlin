// IGNORE_BACKEND_FIR: JVM_IR
open class A<T : Number> {
    open fun foo(t: T) = "A"
}

class Z : A<Int>() {
    override fun foo(t: Int) = "Z"
}


fun box(): String {
    val z = Z()
    val a: A<Int> = z
    return when {
        z.foo(0) != "Z" -> "Fail #1"
        a.foo(0) != "Z" -> "Fail #2"
        else -> "OK"
    }
}
