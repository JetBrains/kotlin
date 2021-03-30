// FIR_IDENTICAL
//KT-2125 Inconsistent error message on UNSAFE_CALL

package e

fun main() {
    val compareTo = 1
    val s: String? = null
    s<!UNSAFE_CALL!>.<!>compareTo("")

    val bar = 2
    s.<!UNRESOLVED_REFERENCE!>bar<!>()
}
