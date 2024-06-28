// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ KT-60136: Spread operator works incorrectly in K1, but correctly in K2.

annotation class A(vararg val xs: String)


@A(*arrayOf("a"), *arrayOf("b"))
fun test() {}
