// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
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

import k.zero
import k.one
import k.two
import k.all

fun test() {
    <!NONE_APPLICABLE!>zero<!>()
    <!NONE_APPLICABLE!>zero<!>(1)
    <!NONE_APPLICABLE!>zero<!>("")

    one()
    one(<!TOO_MANY_ARGUMENTS!>1<!>)
    one(<!TOO_MANY_ARGUMENTS!>""<!>)

    two()
    two(1)
    two(<!ARGUMENT_TYPE_MISMATCH!>""<!>)

    all()
    all(1)
    all("")
}
