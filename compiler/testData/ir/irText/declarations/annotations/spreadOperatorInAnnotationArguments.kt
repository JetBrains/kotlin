// SKIP_SIGNATURE_DUMP
// ^ KT-60136

annotation class A(vararg val xs: String)


@A(*arrayOf("a"), *arrayOf("b"))
fun test() {}
