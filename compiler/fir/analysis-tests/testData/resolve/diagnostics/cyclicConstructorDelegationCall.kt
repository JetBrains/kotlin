class B
class C

class A() {
    constructor(a: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>("test") {}
    constructor(a: String) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(10) {}

    constructor(a: Boolean) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>('\n') {}
    constructor(a: Char) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(0.0) {}
    constructor(a: Double) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(false) {}

    constructor(b: B) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(3.14159265) {}

    constructor(c: C) : this() {}
    constructor(a: List<Int>) : this(C()) {}
}

class D {
    constructor(i: Boolean) {}
    constructor(i: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(3) {}
}

class E<T> {
    // this is not an error about the
    // selection of the proper constructor
    // but a type mismatch for the first
    // argument
    constructor(e: T, i: Int) : <!INAPPLICABLE_CANDIDATE!>this<!>(i, 10) {}
}

class I<T> {
    // this is not an error about the
    // selection of the proper constructor
    // but a type mismatch for the first
    // argument
    constructor(e: T, i: Int) : <!INAPPLICABLE_CANDIDATE!>this<!>(i, 10)
}

class J<T> {
    constructor(e: T, i: Int) : this(i, 10)
    constructor(e: Int, i: Int)
}

class F(s: String) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(i: Boolean)<!> {}
    constructor(i: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(3) {}
}

class G(x: Int) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor()<!> {}
}

class H(x: Int) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor()<!>
}

class K(x: Int) {
    constructor() : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>() {}
}

class M {
    constructor(m: Int)
}

class U : M {
    <!INAPPLICABLE_CANDIDATE!>constructor()<!>
}
