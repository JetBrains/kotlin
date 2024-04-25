// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

var b = true

if (b) {
    val x = 3
}

val y = <!UNRESOLVED_REFERENCE!>x<!>

