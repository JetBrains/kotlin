// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS

interface A {
    private fun foo() = "OK"

    public fun bar() = foo()
}

class B : A {
    private fun foo() = "fail"
}

fun box() = B().bar()