<!CONSTRUCTOR_IN_INTERFACE{LT}!>interface <!CONSTRUCTOR_IN_INTERFACE{PSI}!>A(val s: String)<!><!>

<!CONSTRUCTOR_IN_INTERFACE{LT}!>interface <!CONSTRUCTOR_IN_INTERFACE{PSI}!>B constructor(val s: String)<!><!>

interface C {
    <!CONSTRUCTOR_IN_INTERFACE{LT}!><!CONSTRUCTOR_IN_INTERFACE{PSI}!>constructor(val s: String)<!> {}<!>
}
