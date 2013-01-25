open class A<T : Number>(val t: T) {
    open fun foo(): T = t
}

class Z : A<Int>(17) {
    override fun foo() = 239
}

fun box(): String {
    val z = Z()
    return when {
        z.foo()            != 239 -> "Fail #1"
        (z : A<Int>).foo() != 239 -> "Fail #2"
        else -> "OK"
    }
}
