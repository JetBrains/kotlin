// IGNORE_BACKEND_FIR: JVM_IR
interface I {
    val String.foo: String
        get() = this + ";" + bar()

    fun bar(): String
}

class C : I {
    override fun bar() = "C.bar"

    fun test() = "test".foo
}

fun box(): String {
    val r = C().test()
    if (r != "test;C.bar") return "fail: $r"

    return "OK"
}