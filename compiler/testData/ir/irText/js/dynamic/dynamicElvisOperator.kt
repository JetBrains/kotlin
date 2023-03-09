// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

fun test(d: dynamic) = d ?: "other"
