// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    open fun foo(t: T) = "A"
}

class Z : A<String>() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val z = Z()
    val a: A<String> = z
    return when {
        z.foo("") != "Z" -> "Fail #1"
        a.foo("") != "Z" -> "Fail #2"
        else -> "OK"
    }
}
