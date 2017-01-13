interface Test {
    fun foo()
    val bar: Int
}

fun Test?.foo() {}
val Test?.bar: Int get() = 42