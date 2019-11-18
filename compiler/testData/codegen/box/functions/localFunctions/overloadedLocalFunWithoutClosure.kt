// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun foo(x: String) = x
    fun foo() = foo("K")

    return run {
        foo("O") + foo()
    }
}