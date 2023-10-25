// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!WRONG_MODIFIER_TARGET!>enum<!> class A {
        FOO,
        BAR
    }
    val foo = A.<!UNINITIALIZED_ENUM_ENTRY!>FOO<!>
    val b = object {
        <!WRONG_MODIFIER_TARGET!>enum<!> class B {}
    }
    class C {
        <!WRONG_MODIFIER_TARGET!>enum<!> class D {}
    }
    val f = {
        <!WRONG_MODIFIER_TARGET!>enum<!> class E {}
    }

    <!WRONG_MODIFIER_TARGET!>enum<!> class<!SYNTAX!><!> {}
}
