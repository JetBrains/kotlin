// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -USELESS_CAST

infix fun Any?.foo(a: Any) {}
infix fun Any?.zoo(a: Any) {}
infix fun Any?.Loo(a: Any) {}
infix fun Any?.doo(a: Any) {}
infix fun Any?.ddoo(a: Any) {}
operator fun Any?.contains(a: Any): Boolean = true

fun test(a: Any) {
    1f<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    1f<!UNSUPPORTED!>foo<!> a
    1<!UNSUPPORTED!>doo<!> a
    1<!UNSUPPORTED!>ddoo<!> a
    1<!INFIX_MODIFIER_REQUIRED, UNSUPPORTED!>contains<!> a

    1L<!UNSUPPORTED!>foo<!> a
    1L<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    1L<!UNSUPPORTED!>Loo<!> a

    0b1<!UNSUPPORTED!>foo<!> a
    0b1L<!UNSUPPORTED!>foo<!> a
    0b1L<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    0b1L<!UNSUPPORTED!>Loo<!> a

    0xf<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    0xff<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    0xfL<!UNSUPPORTED!>Loo<!> a

    1.0f<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    1.0f<!UNSUPPORTED!>foo<!> a
    1.0<!UNSUPPORTED!>doo<!> a
    1.0<!UNSUPPORTED!>ddoo<!> a

    .0f<!UNRESOLVED_REFERENCE, UNSUPPORTED!>oo<!> a
    .0f<!UNSUPPORTED!>foo<!> a
    .0<!UNSUPPORTED!>doo<!> a
    .0<!UNSUPPORTED!>ddoo<!> a

    1<!UNSUPPORTED!>in<!> a
    1.0<!UNSUPPORTED!>in<!> a
    1.0f<!UNSUPPORTED!>in<!> a
    1.0<!UNRESOLVED_REFERENCE, UNSUPPORTED!>din<!> a
    .0<!UNSUPPORTED!>in<!> a
    .0f<!UNSUPPORTED!>in<!> a
    .0<!UNRESOLVED_REFERENCE, UNSUPPORTED!>din<!> a

    <!USELESS_IS_CHECK!>1<!UNSUPPORTED!>is<!> Any<!>
    1<!UNSUPPORTED!>as<!> Any
    1<!UNSUPPORTED!>as?<!> Any

    <!USELESS_IS_CHECK!>1<!UNSUPPORTED!>!is<!> Any<!>
    1<!UNSUPPORTED!>!in<!> a
}