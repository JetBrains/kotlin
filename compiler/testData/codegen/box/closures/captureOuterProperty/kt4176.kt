// IGNORE_BACKEND_FIR: JVM_IR
open class Z(val s: Int) {
    open fun a() {}
}

class B(val x: Int) {
    fun foo() {
        class X : Z(x) {

        }
        X()
    }
}

fun box(): String {
    B(1).foo()
    return "OK"
}
