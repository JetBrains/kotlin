// FIR_IDENTICAL
fun foo() {}

val x: Unit? = <!NO_ELSE_IN_WHEN!>when<!> ("A") {
    "B" -> foo()
}
