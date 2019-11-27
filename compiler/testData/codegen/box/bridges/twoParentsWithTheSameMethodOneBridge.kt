// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    fun foo(t: T) = "A"
}

interface B<T> {
    fun foo(t: T) = "B"
}

class Z : A<Int>, B<Int> {
    override fun foo(t: Int) = "Z"
}


fun box(): String {
    val z = Z()
    val a: A<Int> = z
    val b: B<Int> = z
    return when {
        z.foo(0) != "Z" -> "Fail #1"
        a.foo(0) != "Z" -> "Fail #2"
        b.foo(0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
