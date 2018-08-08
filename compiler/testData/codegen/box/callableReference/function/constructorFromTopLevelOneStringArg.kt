class A(val result: String)

fun box() = (::A)("OK").result
