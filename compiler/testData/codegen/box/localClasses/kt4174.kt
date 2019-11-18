// IGNORE_BACKEND_FIR: JVM_IR
open class C(val s: String) {
    fun test(): String {
        return s
    }
}

class B(var x: String) {
    fun foo(): String {
        var s = "OK"
        class Z : C(s) {}
        return Z().test()
    }

    fun foo2(): String {
        class Y : C(x) {}
        return Y().test()
    }
}


fun box(): String {
    val b = B("OK")
    if (b.foo() != "OK") return "fail: ${b.foo()}"
    return b.foo2()
}
