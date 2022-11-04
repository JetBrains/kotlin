interface A<!CONSTRUCTOR_IN_INTERFACE!>(val s: String)<!>

interface B <!CONSTRUCTOR_IN_INTERFACE!>constructor(val s: String)<!>

interface C {
    <!CONSTRUCTOR_IN_INTERFACE!>constructor(s: String)<!> {}
}
