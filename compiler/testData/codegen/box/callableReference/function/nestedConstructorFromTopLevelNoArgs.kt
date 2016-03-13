class A {
    class Nested {
        val result = "OK"
    }
}

fun box() = (A::Nested)().result
