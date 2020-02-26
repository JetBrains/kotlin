// !DIAGNOSTICS: -UNUSED_EXPRESSION
// See KT-27260

class A(val x: String?) {
    fun foo(other: A) {
        when {
            x == null && other.x == null -> "1"
            x.<!INAPPLICABLE_CANDIDATE!>length<!> <!UNRESOLVED_REFERENCE!>><!> 0 -> "2"
        }
    }
}
