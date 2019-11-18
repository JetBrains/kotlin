// IGNORE_BACKEND_FIR: JVM_IR
open class C(s: Int) {
    fun test() {

    }
}

class B(var x: Int) {
    fun foo() {
        class A(val a: Int) : C({a}()) {

        }
        A(11).test()


        class B(val a: Int) : C(a) {
        }

        B(11).test()
    }
}


fun box() : String {
    val b = B(1)
    b.foo()
    return "OK"
}