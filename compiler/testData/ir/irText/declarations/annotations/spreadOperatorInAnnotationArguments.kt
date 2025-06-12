// IGNORE_BACKEND_K1: JVM_IR

annotation class A(vararg val xs: String)

@A(*arrayOf("a"), *arrayOf("b"))
fun test() {}
