// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    fun foo(t: T) = "A"
}

class Z : A<String>


fun box(): String {
    val z = Z()
    val a: A<String> = z
    return when {
        z.foo("") != "A" -> "Fail #1"
        a.foo("") != "A" -> "Fail #2"
        else -> "OK"
    }
}
