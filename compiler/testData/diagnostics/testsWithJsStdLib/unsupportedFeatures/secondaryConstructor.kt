// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    <!SECONDARY_CONSTRUCTOR!>constructor()<!> {}
}

class B {
    <!SECONDARY_CONSTRUCTOR!>constructor()<!> {}
    <!SECONDARY_CONSTRUCTOR!>constructor(a: Int)<!> {}
}

class C(a: Int) {
    <!SECONDARY_CONSTRUCTOR!>constructor()<!> : this(1) {}
    <!SECONDARY_CONSTRUCTOR!>constructor(a: String)<!> : this(2) {}
}
