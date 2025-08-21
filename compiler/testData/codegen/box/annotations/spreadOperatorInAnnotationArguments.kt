// ISSUE: KT-66611
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

annotation class A(vararg val xs: String)
annotation class B(vararg val xa: A)
annotation class C(vararg val xc: C)

@A(*arrayOf("O"), "K")
@B(*arrayOf(A("O", *arrayOf("K"))), A())
@C(*arrayOf(C()))
fun box() = "OK"
