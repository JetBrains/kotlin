// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57436

val <T> T.id get() = this

val test = "abc".id
