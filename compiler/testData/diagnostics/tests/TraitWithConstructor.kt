class C(val a: String) {}

interface T1<!CONSTRUCTOR_IN_TRAIT!>(val x: String)<!> {}

interface T2<!CONSTRUCTOR_IN_TRAIT!>()<!> {}

interface T3 private <!CONSTRUCTOR_IN_TRAIT!>constructor(<!UNUSED_PARAMETER!>a<!>: Int)<!> {}

interface T4 {
    <!CONSTRUCTOR_IN_TRAIT!>constructor(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!> {
        val b: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!> = 1
    }
}

interface T5 private <!CONSTRUCTOR_IN_TRAIT!>()<!> : T4 {}
interface T6 <!CONSTRUCTOR_IN_TRAIT!>private<!><!SYNTAX!><!> : T5 {}
