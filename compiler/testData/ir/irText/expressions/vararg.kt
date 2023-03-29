// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

val test1 = arrayOf<String>()
val test2 = arrayOf("1", "2", "3")
val test3 = arrayOf("0", *test2, *test1, "4")
