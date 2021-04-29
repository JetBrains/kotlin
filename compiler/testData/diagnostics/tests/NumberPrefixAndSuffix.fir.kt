// !DIAGNOSTICS: -UNUSED_PARAMETER -USELESS_CAST

infix fun Any?.foo(a: Any) {}
infix fun Any?.zoo(a: Any) {}
infix fun Any?.Loo(a: Any) {}
infix fun Any?.doo(a: Any) {}
infix fun Any?.ddoo(a: Any) {}
operator fun Any?.contains(a: Any): Boolean = true

fun test(a: Any) {
    1f<!UNRESOLVED_REFERENCE!>oo<!> a
    1ffoo a
    1doo a
    1ddoo a
    1contains a

    1Lfoo a
    1L<!UNRESOLVED_REFERENCE!>oo<!> a
    1LLoo a

    0b1foo a
    0b1Lfoo a
    0b1L<!UNRESOLVED_REFERENCE!>oo<!> a
    0b1LLoo a

    0xf<!UNRESOLVED_REFERENCE!>oo<!> a
    0xff<!UNRESOLVED_REFERENCE!>oo<!> a
    0xfLLoo a

    1.0f<!UNRESOLVED_REFERENCE!>oo<!> a
    1.0ffoo a
    1.0doo a
    1.0ddoo a

    .0f<!UNRESOLVED_REFERENCE!>oo<!> a
    .0ffoo a
    .0doo a
    .0ddoo a

    1in a
    1.0in a
    1.0fin a
    1.0<!UNRESOLVED_REFERENCE!>din<!> a
    .0in a
    .0fin a
    .0<!UNRESOLVED_REFERENCE!>din<!> a

    <!USELESS_IS_CHECK!>1is Any<!>
    1as Any
    1as? Any

    <!USELESS_IS_CHECK!>1!is Any<!>
    1!in a
}
