interface A {
    val foo: Int
    val bar: String
        get() = ""
}

fun test(foo: Int, bar: Int) {
    object : A {
        override val foo: Int = foo + bar
    }
}