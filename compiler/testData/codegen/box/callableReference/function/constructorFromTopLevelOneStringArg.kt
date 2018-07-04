// IGNORE_BACKEND: JVM_IR
class A(val result: String)

fun box() = (::A)("OK").result
