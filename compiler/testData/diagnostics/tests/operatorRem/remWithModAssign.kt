// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object RemAndModAssign {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun modAssign(x: String) {}
    operator fun rem(x: Int) = RemAndModAssign
}

fun test2() {
    var c = RemAndModAssign
    c %= 123
}