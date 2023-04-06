// TARGET_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun test(d: dynamic) = d ?: "other"
