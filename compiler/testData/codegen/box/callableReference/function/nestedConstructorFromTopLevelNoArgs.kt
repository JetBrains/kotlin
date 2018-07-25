// IGNORE_BACKEND: JVM_IR
class A {
    class Nested {
        val result = "OK"
    }
}

fun box() = (A::Nested)().result
