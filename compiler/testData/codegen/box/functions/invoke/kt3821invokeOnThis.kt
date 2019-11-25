// IGNORE_BACKEND_FIR: JVM_IR
//KT-3821 Invoke convention doesn't work for `this`

class A() {
    operator fun invoke() = 42
    fun foo() = this() // Expecting a function type, but found A
}

fun box() = if (A().foo() == 42) "OK" else "fail"