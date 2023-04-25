// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// FIR_DIFFERENCE: KT-55234
// FILE: 1.kt
package k

private fun zero() {}
private fun zero(a: Int) {}
private fun zero(a: String) {}

fun one() {}
private fun one(a: Int) {}
private fun one(a: String) {}

fun two() {}
fun two(a: Int) {}
private fun two(a: String) {}

fun all() {}
fun all(a: Int) {}
fun all(a: String) {}

// FILE: 2.kt

import k.<!INVISIBLE_REFERENCE!>zero<!>
import k.one
import k.two
import k.all

fun test() {
    <!INVISIBLE_REFERENCE!>zero<!>()
    <!INVISIBLE_REFERENCE!>zero<!>(1)
    <!INVISIBLE_REFERENCE!>zero<!>("")

    one()
    <!INVISIBLE_REFERENCE!>one<!>(1)
    <!INVISIBLE_REFERENCE!>one<!>("")

    two()
    two(1)
    <!INVISIBLE_REFERENCE!>two<!>("")

    all()
    all(1)
    all("")
}
