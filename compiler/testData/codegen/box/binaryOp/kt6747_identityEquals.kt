class Test {
    fun check(a: Any?): String {
        if (this === a) return "Fail 1"
        if (!(this !== a)) return "Fail 2"
        return "OK"
    }
}

fun box(): String = Test().check("String")
