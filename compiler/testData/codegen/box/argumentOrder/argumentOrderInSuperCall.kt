// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
var result = "fail"

open class Base(val o: String, val k: String)
class Derived : Base(k = { result = "O"; "K"}() , o = {result += "K"; "O"}()) {}

fun box(): String {
    val derived = Derived()

    if (result != "OK") return "fail $result"
    return derived.o + derived.k
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
