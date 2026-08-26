// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: WASM_JS, WASM_WASI

context(c: String, n: Int)
val topProp: String
    get() = c + n

class A(val tag: String) {
    context(c: String)
    val member: String
        get() = tag + c
}

fun box(): String {
    // --- Top-level property, two bound context arguments, no receiver ---
    val p1 = context("A", 1) { ::topProp }
    val p2 = context("B", 2) { ::topProp }
    val p3 = context("A", 1) { ::topProp }

    if (p1.get() != "A1") return "FAIL invoke p1: ${p1.get()}"
    if (p2.get() != "B2") return "FAIL invoke p2: ${p2.get()}"

    if (p1 == p2) return "FAIL: property references capturing different context arguments compare equal"
    if (p1 != p3) return "FAIL: property references capturing equal context arguments compare unequal"
    if (p1.hashCode() != p3.hashCode()) return "FAIL: equal property references have different hashCodes"

    // --- Class member property: same bound dispatch receiver, different bound context arguments ---
    val a = A("t")
    val m1 = context("X") { a::member }
    val m2 = context("Y") { a::member }
    val m3 = context("X") { a::member }

    if (m1.get() != "tX") return "FAIL invoke m1: ${m1.get()}"

    if (m1 == m2) return "FAIL: member property references capturing different context arguments compare equal"
    if (m1 != m3) return "FAIL: member property references capturing equal context arguments compare unequal"
    if (m1.hashCode() != m3.hashCode()) return "FAIL: equal member property references have different hashCodes"

    return "OK"
}
