// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM_IR, WASM, JS_IR, JS_IR_ES6
// ISSUE: KT-73779
// LANGUAGE: +ContextParameters
class A

class Base {
    context(a: A)
    fun funMember(): String { return "OK" }
}

context(a: A)
fun Base.funMember(): String { return "not OK" }

fun box(): String {
    with(A()) {
        return Base().funMember()
    }
}