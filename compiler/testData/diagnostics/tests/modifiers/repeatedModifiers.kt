abstract <!REPEATED_MODIFIER!>abstract<!> class Foo
public <!REPEATED_MODIFIER!>public<!> class Bar
<!INCOMPATIBLE_MODIFIERS!>open<!> <!REPEATED_MODIFIER!>open<!> <!INCOMPATIBLE_MODIFIERS!>final<!> class Baz {
    private <!REPEATED_MODIFIER!>private<!> fun foo() {}
}

class Bzz(public <!REPEATED_MODIFIER!>public<!> val q: Int = 1) {
    public <!REPEATED_MODIFIER!>public<!> val x: Int = 2

    public val y: Int
        <!REDUNDANT_MODIFIER_IN_GETTER!>public<!> <!REPEATED_MODIFIER!>public<!> get() = 3

    val z: Int
        <!WRONG_MODIFIER_TARGET!>open<!> <!INCOMPATIBLE_MODIFIERS!>final<!> get() = 4

    public <!REPEATED_MODIFIER!>public<!> class B(public <!REPEATED_MODIFIER!>public<!> val z: Int = 1) {
        public <!REPEATED_MODIFIER!>public<!> val y: Int = 2

        public val x: Int
            <!REDUNDANT_MODIFIER_IN_GETTER!>public<!> <!REPEATED_MODIFIER!>public<!> get() = 3
    }

    public <!REPEATED_MODIFIER!>public<!> object C {
        public <!REPEATED_MODIFIER!>public<!> val y: Int = 1
        public <!REPEATED_MODIFIER!>public<!> fun z(): Int = 1
    }
}

public <!REPEATED_MODIFIER!>public<!> val bar: Int = 1

public <!REPEATED_MODIFIER!>public<!> fun foo(): Int = 1

fun test() {
    public <!REPEATED_MODIFIER!>public<!> class B(public <!REPEATED_MODIFIER!>public<!> val z: Int = 1) {
        public <!REPEATED_MODIFIER!>public<!> val y: Int = 2

        public val x: Int
            <!REDUNDANT_MODIFIER_IN_GETTER!>public<!> <!REPEATED_MODIFIER!>public<!> get() = 3
    }
}

