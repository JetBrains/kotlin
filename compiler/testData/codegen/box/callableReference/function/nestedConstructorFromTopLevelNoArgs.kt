// IGNORE_BACKEND_FIR: JVM_IR
class A {
    class Nested {
        val result = "OK"
    }
}

fun box() = (A::Nested)().result
