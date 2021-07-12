// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

interface A {
    val ok get() = "OK"
}
class B : A

fun box(): String {
    context(A) fun result() = ok
    return with(B()) {
        result()
    }
}