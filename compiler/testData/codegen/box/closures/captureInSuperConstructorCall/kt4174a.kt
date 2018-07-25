// IGNORE_BACKEND: JVM_IR
open class C(val s: String) {
    fun test(): String {
        return s
    }
}

class B {
    fun foo(): String {
        var s = "OK"
        class Z : C(s) {}
        return Z().test()
    }
}


fun box() : String {
    val b = B()
    val result = b.foo()
    return result
}