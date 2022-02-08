// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: context receivers aren't yet supported

class A<T>(val a: T)
class B(val b: Any?)

context(A<String>, B) fun f() {
    this@A.a.length
    this@B.b
}

fun box(): String {
    with(A("")) {
        with(B(null)) {
            f()
        }
    }
    return "OK"
}

