// FIR_IDENTICAL
// !LANGUAGE: +NewInference

val Int.plusAssign: (Int) -> Unit
    get() = {}

fun main() {
    1 <!PROPERTY_AS_OPERATOR!>+=<!> 2
}
