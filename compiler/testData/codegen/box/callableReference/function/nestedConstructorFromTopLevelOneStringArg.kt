// IGNORE_BACKEND_FIR: JVM_IR
class A {
    class Nested(val result: String)
}

fun box() = (A::Nested)("OK").result
