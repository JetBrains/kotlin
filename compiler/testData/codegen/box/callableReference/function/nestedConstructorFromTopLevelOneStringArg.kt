class A {
    class Nested(val result: String)
}

fun box() = (A::Nested).let { it("OK") }.result
