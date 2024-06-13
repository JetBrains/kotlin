// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

interface First {
    fun first() {}
}
interface Second
interface Third
interface Fourth

fun chained1(arg: First) = run {
    if (arg !is Second) throw Exception()
    arg
}.let { third ->
    if (third !is Third) throw Exception()
    third
}

fun chained2(arg: First) = run {
    if (arg !is Second) throw Exception()
    arg
}.let { third ->
    if (third !is Third) throw Exception()
    third
}.let { fourth ->
    if (fourth !is Fourth) throw Exception()
    fourth
}

fun test(arg: First) {
    chained1(arg).<!UNRESOLVED_REFERENCE!>first<!>()
    chained2(arg).<!UNRESOLVED_REFERENCE!>first<!>()
}
