// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    fun check(a: Any?): String {
        if (this === a) return "Fail 1"
        if (!(this !== a)) return "Fail 2"
        return "OK"
    }
}

fun box(): String = Test().check("String")
