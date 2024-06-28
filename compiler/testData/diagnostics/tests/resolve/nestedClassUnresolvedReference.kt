// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED

fun ch(
    x1: <!UNRESOLVED_REFERENCE("Foo")!>Foo<!>.Bar,
    x2: <!UNRESOLVED_REFERENCE("Foo")!>Foo<!>.Bar.Baz,
    x3: Outer.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>,
    x4: Outer.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>.Bar,
    x5: Outer.Nested.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>,
    x6: Outer.Nested.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>.Bar,
    x7: Outer.Nested.Nested2.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>,
    x8: Outer.Nested.Nested2.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>.Bar,
    x9: Outer.Inner.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>,
    x10: Outer.O.<!UNRESOLVED_REFERENCE("Foo")!>Foo<!>,
) {}

class Outer {
    class Nested {
        class Nested2
    }

    inner class Inner
    object O
}
