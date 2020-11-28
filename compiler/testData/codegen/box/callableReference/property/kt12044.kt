// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// KT-12044 Assertion "Rewrite at slice LEXICAL_SCOPE" for 'if' with property references

fun box(): String {
    data class Pair<F, S>(val first: F, val second: S)
    val (x, y) =
            Pair(1,
                 if (1 == 1)
                     Pair<String, String>::first
                 else
                     Pair<String, String>::second)
    return y.get(Pair("OK", "Fail"))
}
