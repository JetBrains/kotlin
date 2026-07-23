// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: WASM_JS, WASM_WASI

context(c: String?)
fun orDefault(): String = c ?: "default"

fun box(): String {
    val n1 = context<String?, () -> String>(null) { ::orDefault }
    val n2 = context<String?, () -> String>("x") { ::orDefault }
    val n3 = context<String?, () -> String>(null) { ::orDefault }

    if (n1() != "default") return "FAIL 1: ${n1()}"
    if (n2() != "x") return "FAIL 2: ${n2()}"

    if (n1 == n2) return "FAIL 3: references capturing null and non-null context arguments compare equal"
    if (n1 != n3) return "FAIL 4: references capturing null context arguments compare unequal"
    if (n1.hashCode() != n3.hashCode()) return "FAIL 5: equal references have different hashCodes"

    return "OK"
}
