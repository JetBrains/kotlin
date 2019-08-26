// IGNORE_BACKEND: WASM
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
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
