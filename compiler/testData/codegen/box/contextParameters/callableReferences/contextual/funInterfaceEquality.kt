// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: WASM_JS, WASM_WASI

fun interface F {
    fun run(): String
}

fun id(f: F): F = f

context(a: String, b: Int)
fun foo(): String = a + b

object O {
    context(a: String, b: Int)
    fun bar(suffix: String = "!"): String = a + b + suffix
}

fun box(): String {
    val f1: F = context("A", 1) { id(::foo) }
    val f2: F = context("B", 2) { id(::foo) }
    val f3: F = context("A", 1) { id(::foo) }

    if (f1.run() != "A1") return "FAIL invoke f1: ${f1.run()}"
    if (f2.run() != "B2") return "FAIL invoke f2: ${f2.run()}"

    if (f1 == f2) return "FAIL: fun interface wrappers capturing different context arguments compare equal"
    if (f1 != f3) return "FAIL: fun interface wrappers capturing equal context arguments compare unequal"
    if (f1.hashCode() != f3.hashCode()) return "FAIL: equal fun interface wrappers have different hashCodes"

    val a1: F = context("A", 1) { id(O::bar) }
    val a2: F = context("B", 2) { id(O::bar) }
    val a3: F = context("A", 1) { id(O::bar) }

    if (a1.run() != "A1!") return "FAIL invoke a1: ${a1.run()}"
    if (a2.run() != "B2!") return "FAIL invoke a2: ${a2.run()}"

    if (a1 == a2) return "FAIL: adapted fun interface wrappers capturing different context arguments compare equal"
    if (a1 != a3) return "FAIL: adapted fun interface wrappers capturing equal context arguments compare unequal"
    if (a1.hashCode() != a3.hashCode()) return "FAIL: equal adapted fun interface wrappers have different hashCodes"

    return "OK"
}
