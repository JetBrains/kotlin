// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <vararg Ts> variadicFn(first: String, vararg args: *Ts) {}

fun test() {
    variadicFn("foo", "bar", 14)
}