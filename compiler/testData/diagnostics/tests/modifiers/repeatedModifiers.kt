<!REPEATED_MODIFIER!>abstract<!> <!REPEATED_MODIFIER!>abstract<!> class Foo
<!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> class Bar
<!REPEATED_MODIFIER, INCOMPATIBLE_MODIFIERS!>open<!> <!REPEATED_MODIFIER!>open<!> <!INCOMPATIBLE_MODIFIERS!>final<!> class Baz {
    <!REPEATED_MODIFIER!>private<!> <!REPEATED_MODIFIER!>private<!> fun foo() {}
}

class Bzz(<!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val q: Int = 1) {
    <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val x: Int = 2

    public val y: Int
        <!REPEATED_MODIFIER, REDUNDANT_MODIFIER_IN_GETTER!>public<!> <!REPEATED_MODIFIER!>public<!> get() = 3

    val z: Int
        <!INCOMPATIBLE_MODIFIERS, ILLEGAL_MODIFIER!>open<!> <!INCOMPATIBLE_MODIFIERS, ILLEGAL_MODIFIER!>final<!> get() = 4

    <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> class B(<!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val z: Int = 1) {
        <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val y: Int = 2

        public val x: Int
            <!REPEATED_MODIFIER, REDUNDANT_MODIFIER_IN_GETTER!>public<!> <!REPEATED_MODIFIER!>public<!> get() = 3
    }

    <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> object C {
        <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val y: Int = 1
        <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> fun z(): Int = 1
    }
}

<!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val bar: Int = 1

<!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> fun foo(): Int = 1

fun test() {
    <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> class B(<!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val z: Int = 1) {
        <!REPEATED_MODIFIER!>public<!> <!REPEATED_MODIFIER!>public<!> val y: Int = 2

        public val x: Int
            <!REPEATED_MODIFIER, REDUNDANT_MODIFIER_IN_GETTER!>public<!> <!REPEATED_MODIFIER!>public<!> get() = 3
    }
}

