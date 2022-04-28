// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

var result = ""

class C

operator fun C.assign(a: String) {
    result = a
}

fun test() {
    val c = C()
    c = "hello"
    <!VAL_REASSIGNMENT!>c<!> = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}
