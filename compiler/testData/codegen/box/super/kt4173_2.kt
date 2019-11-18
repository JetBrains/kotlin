// IGNORE_BACKEND_FIR: JVM_IR
open class X(var s: ()-> Unit)

open class C(val f: X) {
    fun test() {
        f.s()
    }
}

class B(var x: Int) {
    fun foo() {
        object : C(object: X({x = 3}) {}) {}.test()
    }
}


fun box() : String {
    val b = B(1)
    b.foo()
    return if (b.x != 3) "fail: b.x = ${b.x}" else "OK"
}