// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

val <T> T.id get() = this

val test = "abc".id
