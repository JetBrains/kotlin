open class A<T : U, U> {
    open fun foo(t: T, u: U) = "A"
}

open class B : A<Int, Number>()

class Z : B() {
    override fun foo(t: Int, u: Number) = "Z"
}

fun box(): String {
    val z = Z()
    return when {
        z.foo(0, 0)                    != "Z" -> "Fail #1"
        (z : B).foo(0, 0)              != "Z" -> "Fail #2"
        (z : A<Int, Number>).foo(0, 0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
