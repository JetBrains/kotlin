interface G<T> {
    fun foo()
    val bar: Int
}

fun G.foo() {}
val G.bar: Int get() = 42