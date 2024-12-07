// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(
    p: <!UNRESOLVED_REFERENCE!>Promise<!><*>,
    d: <!UNRESOLVED_REFERENCE!>Date<!>,
    c: <!UNRESOLVED_REFERENCE!>Console<!>,
    r: <!UNRESOLVED_REFERENCE!>RegExp<!>,
    rm: <!UNRESOLVED_REFERENCE!>RegExpMatch<!>,
    j: <!UNRESOLVED_REFERENCE!>Json<!>,
) {
    <!UNRESOLVED_REFERENCE!>json<!>()
}
