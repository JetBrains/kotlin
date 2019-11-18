// IGNORE_BACKEND_FIR: JVM_IR
interface A<T, U> {
    fun foo(t: T, u: U) = "A"
}

interface B<U> : A<String, U>

interface C<T> : A<T, Int>

class Z : B<Int>, C<String> {
    override fun foo(t: String, u: Int) = "Z"
}


fun box(): String {
    val z = Z()
    val c: C<String> = z
    val b: B<Int> = z
    val a: A<String, Int> = z
    return when {
        z.foo("", 0) != "Z" -> "Fail #1"
        c.foo("", 0) != "Z" -> "Fail #2"
        b.foo("", 0) != "Z" -> "Fail #3"
        a.foo("", 0) != "Z" -> "Fail #4"
        else -> "OK"
    }
}
