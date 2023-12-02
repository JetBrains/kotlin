// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

infix fun Any?.foo(a: Any) {}
operator fun Any?.contains(a: Any): Boolean = true

fun test(a: Any) {

    a <!UNSUPPORTED!>foo<!>""
    a <!UNSUPPORTED!>foo<!>"asd"
    a <!UNSUPPORTED!>foo<!>"$a"
    a <!UNSUPPORTED!>foo<!>"asd${a}sfsa"
    a <!UNSUPPORTED!>foo<!>"""sdf"""
    a <!UNSUPPORTED!>foo<!>'d'
    a <!UNSUPPORTED!>foo<!><!EMPTY_CHARACTER_LITERAL!>''<!>

    a <!UNSUPPORTED!>foo<!>""<!UNSUPPORTED!>foo<!> a
    a <!UNSUPPORTED!>foo<!>"asd"<!UNSUPPORTED!>foo<!> a
    a <!UNSUPPORTED!>foo<!>"$a"<!UNSUPPORTED!>foo<!> a
    a <!UNSUPPORTED!>foo<!>"asd${a}sfsa"<!UNSUPPORTED!>foo<!> a
    a <!UNSUPPORTED!>foo<!>"""sdf"""<!UNSUPPORTED!>foo<!> a
    a <!UNSUPPORTED!>foo<!>'d'<!UNSUPPORTED!>foo<!> a
    a <!UNSUPPORTED!>foo<!><!EMPTY_CHARACTER_LITERAL!>''<!><!UNSUPPORTED!>foo<!> a

    a <!UNSUPPORTED!>in<!>"foo"
    a <!UNSUPPORTED!>in<!>"""foo"""
    a <!UNSUPPORTED!>in<!>'s'
    a <!UNSUPPORTED!>in<!><!EMPTY_CHARACTER_LITERAL!>''<!>

    a <!UNSUPPORTED!>!in<!>"foo"
    a <!UNSUPPORTED!>!in<!>"""foo"""
    a <!UNSUPPORTED!>!in<!>'s'
    a <!UNSUPPORTED!>!in<!><!EMPTY_CHARACTER_LITERAL!>''<!>

    if(<!USELESS_IS_CHECK!>"s"<!UNSUPPORTED!>is<!> Any<!>) {}
    if(<!USELESS_IS_CHECK!>"s"<!UNSUPPORTED!>is<!> Any<!>) {}
    test("s"<!UNSUPPORTED!>as<!> Any)

    a <!UNSUPPORTED!>foo<!>""<!SYNTAX, UNSUPPORTED!>1<!>
    a <!UNSUPPORTED!>foo<!>""<!SYNTAX, UNSUPPORTED!>1.0<!>
}