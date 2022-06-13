class A(val result: String)

fun box() = (::A).let { it("OK") }.result
