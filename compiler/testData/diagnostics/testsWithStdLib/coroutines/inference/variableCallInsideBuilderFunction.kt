// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.Experimental

fun test(x: List<Int>) {
    x + x
}