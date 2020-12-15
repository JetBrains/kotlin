class A(x: Int) {
    constructor(z: String) : this(10)
}

class B : A {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED, NONE_APPLICABLE{PSI}!>constructor()<!><!NONE_APPLICABLE{LT}!><!>
    constructor(z: String) : this()
}

<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class C : A(20) {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED, NONE_APPLICABLE{PSI}!>constructor()<!><!NONE_APPLICABLE{LT}!><!>
    constructor(z: String) : this()
}<!>

class D() : A(20) {
    <!NONE_APPLICABLE{PSI}, PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED{PSI}!>constructor(x: Int)<!><!NONE_APPLICABLE{LT}, PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED{LT}!><!>
    constructor(z: String) : this()
}
