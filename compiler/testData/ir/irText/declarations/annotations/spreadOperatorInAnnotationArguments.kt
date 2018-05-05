annotation class A(vararg val xs: String)


@A(*arrayOf("a"), *arrayOf("b"))
fun test() {}