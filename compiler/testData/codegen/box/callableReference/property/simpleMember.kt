class A(val x: Int)

fun box(): String {
    val p = A::x
    if (p.get(A(42)) != 42) return "Fail 1"
    if (p.get(A(-1)) != -1) return "Fail 2"
    if (p.name != "x") return "Fail 3"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
