// WITH_STDLIB
// ISSUE: KT-80243
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

fun box(): String {
    for ((index, value) in arrayOf("").withIndex()) {}
    return "OK"
}