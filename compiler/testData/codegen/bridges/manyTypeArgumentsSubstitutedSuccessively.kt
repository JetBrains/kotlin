open class A<T, U, V> {
    open fun foo(t: T, u: U, v: V) = "A"
}

open class B<T, V> : A<T, Int, V>()

open class C<V> : B<String, V>()

class Z : C<Double>() {
    override fun foo(t: String, u: Int, v: Double) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("", 0, 0.0)                            != "Z" -> "Fail #1"
        (z : C<Double>).foo("", 0, 0.0)              != "Z" -> "Fail #2"
        (z : B<String, Double>).foo("", 0, 0.0)      != "Z" -> "Fail #3"
        (z : A<String, Int, Double>).foo("", 0, 0.0) != "Z" -> "Fail #4"
        else -> "OK"
    }
}
