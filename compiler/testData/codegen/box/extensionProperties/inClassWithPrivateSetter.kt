// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    var storage = "Fail"

    var Int.foo: String
        get() = storage
        private set(str: String) {
            storage = str
        }

    fun test(): String {
        val i = 1
        i.foo = "OK"
        return i.foo
    }
}

fun box(): String {
    return Test().test()
}
