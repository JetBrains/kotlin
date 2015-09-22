// !DIAGNOSTICS: -MISSING_CONSTRUCTOR_KEYWORD

class C(val a: String) {}

interface T1<!CONSTRUCTOR_IN_TRAIT!>(val x: String)<!> {}

interface T2 <!CONSTRUCTOR_IN_TRAIT!>constructor()<!> {}

interface T3 <!CONSTRUCTOR_IN_TRAIT!>constructor(<!UNUSED_PARAMETER!>a<!>: Int)<!> {}

interface T4 {
    <!CONSTRUCTOR_IN_TRAIT!>constructor(<!UNUSED_PARAMETER!>a<!>: Int)<!> {
        val <!UNUSED_VARIABLE!>b<!>: Int = 1
    }
}

interface T5 <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> <!CONSTRUCTOR_IN_TRAIT!>()<!> : T4 {}
interface T6 <!CONSTRUCTOR_IN_TRAIT!>public<!><!SYNTAX!><!> : T5 {}