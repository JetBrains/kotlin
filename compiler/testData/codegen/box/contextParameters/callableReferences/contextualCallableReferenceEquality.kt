// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: WASM_JS, WASM_WASI
// ISSUE: KT-86452

context(a: String, b: Int)
fun foo(): String = a + b

object O {
    context(a: String, b: Int)
    fun bar(suffix: String = "!"): String = a + b + suffix
}

fun box(): String {
    // --- Plain references (FunctionReferenceImpl) ---
    // Two context parameters (of distinct types, to avoid AMBIGUOUS_CONTEXT_ARGUMENT) are bound,
    // so both land in `boundContextArguments` and the runtime `receiver` is NO_RECEIVER.
    val r1: () -> String = context("A", 1) { ::foo }
    val r2: () -> String = context("B", 2) { ::foo }
    val r3: () -> String = context("A", 1) { ::foo }

    if (r1() != "A1") return "FAIL invoke r1: ${r1()}"
    if (r2() != "B2") return "FAIL invoke r2: ${r2()}"

    // Different captured context arguments => the references must differ.
    if (r1 == r2) return "FAIL: plain references capturing different context arguments compare equal"
    // Same captured context arguments => the references must be equal, with equal hashCodes.
    if (r1 != r3) return "FAIL: plain references capturing equal context arguments compare unequal"
    if (r1.hashCode() != r3.hashCode()) return "FAIL: equal plain references have different hashCodes"

    // --- Adapted references (AdaptedFunctionReference; the default `suffix` argument is dropped) ---
    // The expected type must be present right at the reference for the default-argument adapter to resolve,
    // so the reference is bound to a typed `val` inside the context block (as in contextualCallableReference.kt).
    val a1: () -> String = context("A", 1) { val r: () -> String = O::bar; r }
    val a2: () -> String = context("B", 2) { val r: () -> String = O::bar; r }
    val a3: () -> String = context("A", 1) { val r: () -> String = O::bar; r }

    if (a1() != "A1!") return "FAIL invoke a1: ${a1()}"
    if (a2() != "B2!") return "FAIL invoke a2: ${a2()}"

    // Different captured context arguments => the adapted references must differ.
    if (a1 == a2) return "FAIL: adapted references capturing different context arguments compare equal"
    // Same captured context arguments => the adapted references must be equal, with equal hashCodes.
    if (a1 != a3) return "FAIL: adapted references capturing equal context arguments compare unequal"
    if (a1.hashCode() != a3.hashCode()) return "FAIL: equal adapted references have different hashCodes"

    return "OK"
}
