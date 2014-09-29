//KT-2125 Inconsistent error message on UNSAFE_CALL

package e

fun main() {
    val <!UNUSED_VARIABLE!>compareTo<!> = 1
    val s: String? = null
    s<!UNSAFE_CALL!>.<!>compareTo("")

    val <!UNUSED_VARIABLE!>bar<!> = 2
    s.<!UNRESOLVED_REFERENCE!>bar<!>()
}
