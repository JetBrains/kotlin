open class A(x: Int) {
    constructor(z: String) : this(10)
}

class B : A {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor()<!>
    constructor(z: String) : this()
}

<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class C : A(20) {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor()<!>
    constructor(z: String) : <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>()
}<!>

class D() : A(20) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(x: Int)<!>
    constructor(z: String) : this()
}
