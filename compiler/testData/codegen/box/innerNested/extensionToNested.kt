// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    class Nested {
        val value = "OK"
    }
}

fun Test.Nested.foo() = value

fun box() = Test.Nested().foo()
