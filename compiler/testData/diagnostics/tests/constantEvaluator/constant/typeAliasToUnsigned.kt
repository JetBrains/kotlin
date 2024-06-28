// FIR_IDENTICAL
// WITH_STDLIB

typealias UI = UInt

const val a: UI = 1u
const val b: UI = a
const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>a == b<!>
