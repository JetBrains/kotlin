// WITH_STDLIB
// SKIP_KT_DUMP
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun test(): Boolean {
    val ref = (listOf('a') + "-")::contains
    return ref('a')
}
