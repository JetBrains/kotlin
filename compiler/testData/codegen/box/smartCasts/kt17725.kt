// IGNORE_BACKEND_FIR: JVM_IR
class Bob {
    fun Bob.bar() = "OK"
}

fun Any.foo() = when(this) {
    is Bob -> bar()
    else -> throw AssertionError()
}

fun box(): String = Bob().foo()