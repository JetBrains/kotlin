// RUN_PIPELINE_TILL: FRONTEND
open class A(x: Int) {
    constructor(z: String) : this(10)
}

class B : A {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor()<!>
    constructor(z: String) : this()
}

class <!CONFLICTING_OVERLOADS!>C<!> : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>A<!>(20) {
    <!CONFLICTING_OVERLOADS, EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor()<!>
    constructor(z: String) : <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>()
}

class D() : A(20) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(x: Int)<!>
    constructor(z: String) : this()
}
