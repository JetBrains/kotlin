// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.NOT_NULL)
fun nullWhenNull(@Equals(ConstantValue.NOT_NULL) x: Int?) = x?.inc()

fun testNullWhenNull(x: Int?) {
    if (nullWhenNull(x) == null) {
        x<!UNSAFE_CALL!>.<!>dec()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }

    if (nullWhenNull(x) != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    } else {
        x<!UNSAFE_CALL!>.<!>dec()
    }

    x<!UNSAFE_CALL!>.<!>dec()
}

// NB. it is the same function as `nullWhenNull`, but annotations specifies other facet of the function behaviour
@Returns(ConstantValue.NULL)
fun notNullWhenNotNull (@Equals(ConstantValue.NULL) x: Int?) = x?.inc()

fun testNotNullWhenNotNull (x: Int?) {
    if (notNullWhenNotNull(x) == null) {
        <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>
    } else {
        x<!UNSAFE_CALL!>.<!>dec()
    }

    if (notNullWhenNotNull(x) != null) {
        x<!UNSAFE_CALL!>.<!>dec()
    } else {
        <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>
    }

    x<!UNSAFE_CALL!>.<!>dec()
}