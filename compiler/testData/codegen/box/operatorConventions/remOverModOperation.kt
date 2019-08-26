// !LANGUAGE: -ProhibitOperatorMod
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

class A() {
    var x = 5

    operator fun mod(y: Int) { throw RuntimeException("mod has been called instead of rem") }
    operator fun rem(y: Int) { x -= y }
}

fun box(): String {
    val a = A()

    a % 5

    if (a.x != 0) {
        return "Fail: a.x(${a.x}) != 0"
    }

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ RuntimeException 
