// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
annotation class ann

fun test(@ann p: Int) {

}

val bar = fun(@ann g: Int) {}
