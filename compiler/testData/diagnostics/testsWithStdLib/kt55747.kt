// FIR_IDENTICAL

object Rem {
    <!FORBIDDEN_BINARY_MOD!>operator<!> fun mod(x: Int) {}
    <!FORBIDDEN_BINARY_MOD!>operator<!> fun modAssign(x: Int) {}
}
