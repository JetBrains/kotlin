// WITH_STDLIB
// SKIP_KT_DUMP
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// KT-61141: For result of `+`, Native backend inferred type Comparable instead of Nothing
// IGNORE_BACKEND: NATIVE

fun test(): Boolean {
    val ref = (listOf('a') + "-")::contains
    return ref('a')
}
