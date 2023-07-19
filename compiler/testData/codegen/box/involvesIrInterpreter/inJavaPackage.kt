// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

package java2d

class A {
    fun getConst() = <!EVALUATED("OK")!>OK<!>

    companion object {
        const val OK = <!EVALUATED("OK")!>"OK"<!>
    }
}

fun box(): String {
    return A().getConst()
}
