// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    private val Int.foo: String
        get() {
            return "OK"
        }

    fun test(): String {
        return 1.foo
    }
}

fun box(): String {
    return Test().test()
}
