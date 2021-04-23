// FIR_IDENTICAL
//KT-4341 No resolved call for right-hand side of equals expression
package g

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U) {
    <!USAGE_IS_NOT_INLINABLE!>s<!> == <!USAGE_IS_NOT_INLINABLE!>s<!> //resolved call for right-hand side 's' not traced
}
