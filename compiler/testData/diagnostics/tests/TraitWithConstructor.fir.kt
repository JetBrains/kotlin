// !DIAGNOSTICS: -MISSING_CONSTRUCTOR_KEYWORD

class C(val a: String) {}

interface <!CONSTRUCTOR_IN_INTERFACE!>T1(val x: String)<!> {}

interface <!CONSTRUCTOR_IN_INTERFACE!>T2 constructor()<!> {}

interface <!CONSTRUCTOR_IN_INTERFACE!>T3 private constructor(a: Int)<!> {}

interface T4 {
    <!CONSTRUCTOR_IN_INTERFACE!>constructor(a: Int)<!> {
        val b: Int = 1
    }
}

interface <!CONSTRUCTOR_IN_INTERFACE!>T5 private ()<!> : T4 {}
interface <!CONSTRUCTOR_IN_INTERFACE!>T6<!> private<!SYNTAX!><!> : T5 {}