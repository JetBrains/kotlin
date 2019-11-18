// IGNORE_BACKEND_FIR: JVM_IR
var state: String = "Fail"

class A {
    private constructor(x: String = "OK") {
        state = x
    }

    companion object {
        fun foo() = A()
    }
}

fun box(): String {
    A.foo()
    return state
}
